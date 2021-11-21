package ru.sxtim.chat.network;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** TCPConnection - класс который реализует одно TCP соединение,
 *  для того чтобы не работать на прямую с ServerSocket и Socket;
 */
public class TCPConnection {
    //========Fields===========
    // сокет
    private final Socket socket;
    // Поток, который будет слушать входящее сообщение.
    // Т. е. один поток на каждом клиенте и он будет слушать входящие сообщения
    // постоянно читать поток ввода, если строчка прилетела, то будет генерировать события
    private final Thread rxThread;

    // Слушатель событий
    private final TCPConnectionListener eventListener;
    //потоки для работы со строками
    // поток ввода
    private final BufferedReader in;
    // поток вывода
    private final BufferedWriter out;

    //=============Constructors============

    /**
     * Для того чтобы класс TCPConnection был универсальным (возможность использования и в серверной части и в клиентской),
     * серверная часть должна отработать каким-то одним образом, а клиентская другим;
     * Например, если серверу пришло сообщение он его должен разослать всем
     * клиентам, если клиенту - то допустим написать себе куда-то в окшко;
     * Для этого в java применяется особый вид абстракции - Интерфейсы;
     * Придумаем событийную систему и опишем в интерфейсе TCPConnectionListener
     */
    // ПЕРВЫЙ КОНСТРУКТОР рассчитан на то, сокет уже создан, что кто-то снаружи сделает соединение
    // принимает на вход готовый объект сокета
    // и создает с этим сокетом соединение
    // принимает на вход экземпляр слушателя событий
    public TCPConnection( TCPConnectionListener eventListener, Socket socket ) throws IOException {
/**запоминаем сокет и слушателя событий в поля*/
        this.eventListener = eventListener;
        this.socket = socket;
/** далее у этого сокета получаем входящий и исходящий поток  socket.getInputStream();
*чтобы принимать какие-то байты и писать какие-то байты     socket.getInputStream();
*/        // На основе простого потока getInputStream создаем более сложный InputStream
        // и оборачиваем в экземпляр класса BufferedReader, который умеет читать строчки.
        // Можно напрямую указать кодировку с которой работаем
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        /**Поток должен что-то выполнять, для того чтобы он что-то выполнял - один из вариантов
         * передать ему экземпляр класса, который реализует интерфейс Runnable;
         * Создаем анонимный класс, который реализует интерфейс Runnable, оверрайдим метод run()
         * исоздаем его экземпляр
        */
        // создаем новый поток, который слушает все входящее
        rxThread = new Thread(new Runnable() {
            @Override
            public void run() {//когда стартовал поток
                try {// предаем в соединение себя (экземпляр обрамляющего класса)
                    eventListener.onConnectionReady(TCPConnection.this);

                    // получаем строчки в бесконечном цикле (любое сетевое соединение как правило - бесконечный цикл)
                    while(!rxThread.isInterrupted()){// пока поток не прерван
                        // читаем строку
                        String msg = in.readLine();
                        //  и отдаем ее eventListener (передаем туда объект соединения и строчку)
                        eventListener.onReceiveString(TCPConnection.this, msg);
                        System.out.println("iSInterrupted");
                    }


                } catch(IOException e){
                    eventListener.onException(TCPConnection.this, e);
                }
                // если случилась какая-то ошибка в любом случае закрываем сокет
                // передаем дисконнект
                finally {
                    eventListener.onDisconnect(TCPConnection.this);
                }
            }
        });
        // запускаем поток
        rxThread.start();
    }
    // ВТОРОЙ КОНСТРУКТОР создает сокет. Рассчитан на то что сокет будет создаваться внутри
    // передаем ipAddr, port
    public TCPConnection(TCPConnectionListener eventListener, String ipAddr, int port ) throws IOException{
        //вызываем ПЕРВЫЙ конструктор
        // передаем сокет на основании ipAddr, и порта
        this(eventListener, new Socket(ipAddr, port));

    }

    // =======Functions=======

    /**
     * Так как класс многопоточный, синхронизируем методы для потокобезопасности,
     * чтобы к этим методам можно было обращаться из разных потоков
     * мы их синхронизируем
     */
    // отправить сообщение (спрашивает строчку которую мыы хотим отправить)
    public synchronized void sendString(String value){
        try {


            // Пишем в поток вывода.
            // Добавляем символы конца строки - возврат каретки и перевод строки,
            // для того чтоб понять, где конец строки
            out.write(value + "\n");
            // сбрасывает все буферы и отправляет
            out.flush();
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
            // так как случилось исключение мы разрываем поток
            disconnect();
        }
    }

    // оборвать соединение (чтобы снаружи порвать соединение в любой момент)
    public synchronized void  disconnect (){
        // прерываем поток
        rxThread.interrupt();
        // закрываем поток
        try {
            socket.close();
        } catch (IOException e) {
            // передаем Евентлистенеру обработку исключения
            eventListener.onException(TCPConnection.this, e);
        }
    }

    // овверайдим toString, чтобы видеть кто подключился/отключился (стандартная реализация полиморфизма)
    @Override
    public String toString (){
        // адрес с которого установлено соединение и номер порта
        return "TCPConnection: " + socket.getInetAddress() + ": " + socket.getPort();
    }


}
