package ru.sxtim.chat.server;

import ru.sxtim.chat.network.TCPConnection;
import ru.sxtim.chat.network.TCPConnectionListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

/** Server  - некая сущность которая может принимать сообщения и рассылать нескольким клиентам
* Может держать несколько соединений активными;
* Взаимодействие с сетью происходит с помощью двух основных классов: ServerSocket, Socket;
* ServerSocket умеет слушать входящее соединение, создавать объект сокета, который
 связан с этим соединением, и готовый сокет отдавать;
* Socket устанавливает соединение;
 *
 *Делаем ChatServer слушателем с помощью реализации интерфейса TCPConnectionListener
 * ChatServer является одновременно и ChatServer и TCPConnectionListener

*/
public class ChatServer implements TCPConnectionListener {
    public static void main(String[] args) {
        new ChatServer();
    }
    //============ Fields ==============
    //Для неограниченного количества соединений создаем ArrayList
    // реализует функционал списка
    private final ArrayList<TCPConnection> connections = new ArrayList<>();


    // ===========Constructors============
    private ChatServer() {
        System.out.println("Server running...");
        // создаем сервер сокет, который слушает порт 8189
        try(ServerSocket serverSocket = new ServerSocket(8189)){// используем try with resources,
                                                                        //он умеет закрывать ресурсы, которые захвачены
            /** Cлушаем входящее соединение;
            * В бесконечном цикле висим в методе accept()(ждет нового соединения и как только это соединение установилось,
            * возвращает объект сокета, который связан с этим соединением)
            * Тут же передаем этот объект сокета в конструктор TCPConnection включая себя как Listenera, создаем экземпляр TCPConnectiona
             */
            while(true){
                // ловим исключение при подключении клиента
                try{
                    //как слушатель можем передать туда себя и объект сокета(при входящем соединение возвращает метод accept())
                    new TCPConnection(this, serverSocket.accept());
                } catch(IOException e){
                    //если что-то случается при подключении клиентов - просто логируем
                    System.out.println("TCPConnection exception: " + e);
                }
            }



        } catch (Exception e){
            // поднимаем исключение доверху с другим классом и
            // роняем приложение
            throw new RuntimeException(e);
        }
    }

    /** Описываем реакции на события
     * Так как клиентов будет очень много - синхронизируем методы (чтобы из разных потоков в них нельзя было попасть)
     */


    @Override// когда Connection готов, то добавляем его в список соединений
    public synchronized void onConnectionReady(TCPConnection tcpConnection) {
        connections.add(tcpConnection);
        // если клиент подключился, то всех оповещаем
        sendToAllConnections("Client connected: " + tcpConnection);
    }

    @Override// если приняли строчку - нужно разослать всем клиентам
    public  synchronized void onReceiveString(TCPConnection tcpConnection, String value) {
        // отправляем всем принятую строчку
        sendToAllConnections(value);
    }

    @Override// если Connection отвалился, то удаляем его из списка соединений
    public  synchronized void onDisconnect(TCPConnection tcpConnection) {
        connections.remove(tcpConnection);
        sendToAllConnections("Client connected: " + tcpConnection);
    }

    @Override// если исключение пишем в консоль
    public synchronized void onException(TCPConnection tcpConnection, Exception e) {
        System.out.println("TCPConnection exception: " + e);
    }

    // метод, который рассылает всем сообщения (кто подключился, кто отключился)
    private void sendToAllConnections(String value){
        // логируем строчку в консоль, которую отправляем
        System.out.println(value);
        // проходим по всему списку соединений и отправляем сообщение
        final int cnt = connections.size();// получаем размер списка в переменную, чтобы не вызывался каждый раз метод size()(оптимизация)
        for (int i = 0; i < connections.size(); i++) connections.get(i).sendString(value);
    }
}
