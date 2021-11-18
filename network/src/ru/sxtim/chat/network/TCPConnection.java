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

    // принимает на вход готовый объект сокета
    // и создает с этим сокетом соединение
    // передаем экземпляр слушателя событий
    public TCPConnection(Socket socket, TCPConnectionListener eventListener) throws IOException {
/**запоминаем сокет и слушателя событий в поля*/
        this.eventListener = eventListener;
        this.socket = socket;
/** далее у этого сокета получаем входящий и исходящий поток  socket.getInputStream();
*чтобы принимать какие-то байты и писать какие-то байты     socket.getInputStream();
*/
        // на основе простого потока getInputStream создаем более сложный InputStream
        // и оборачиваем в экземпляр класса BufferedReader, который умеет читать строчки
        // можно напрямую указать кодировку с которой работаем
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
                    }


                } catch(IOException e){

                }
                // если случилась какая то ошибка в любом случае закрываем сокет
                finally {

                }
            }
        });
        // запускаем поток
        rxThread.start();
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
            // пишем в поток вывода
            out.write(value);
        } catch (IOException e) {
            eventListener.onException(TCPConnection.this, e);
            // так как случилось исключение мы разрываем поток
            disconnect();
        }
    }

    // оборвать соединение (чтобы снаружи порвать соединение в любой момент)
    public synchronized void  disconnect (){
        // прерываем поток
        rxThread.isInterrupted();
        // закрываем поток
        try {
            socket.close();
        } catch (IOException e) {
            // передаем Евентлистенеру обработку исключения
            eventListener.onException(TCPConnection.this, e);
        }
    }


}