package ru.sxtim.chat.client;

import org.w3c.dom.ls.LSOutput;
import ru.sxtim.chat.network.TCPConnection;
import ru.sxtim.chat.network.TCPConnectionListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;


/** Так как со Swing можно работать только из потока EDT
 *
 */
public class ClientWindow extends JFrame implements ActionListener, TCPConnectionListener {
    public static void main(String[] args) {

        //Так как со Swing можно работать только из потока EDT
        // выполняем в потоке EDT
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientWindow();
            }
        });

    }

    //Fields
    private static final String IP_ADDR = "127.0.0.1";
    private static final int PORT = 8189;
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;
    private TCPConnection connection;

    // поле для сообщений
    private final JTextArea chatLog = new JTextArea();
    private final JTextField fieldNickName = new JTextField("BOB");
    private final JTextField fieldInputMsg = new JTextField();

    // Constructors

    private ClientWindow() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        chatLog.setEditable(false);//запрет редактирования
        chatLog.setLineWrap(true);// перенос слов
        add(chatLog, BorderLayout.CENTER);
        add(fieldNickName, BorderLayout.NORTH);
        //добавляем себя, чтобы перехватывать нажатия Enter
        fieldInputMsg.addActionListener(this);
        add(fieldInputMsg, BorderLayout.SOUTH);

        try {
            connection = new TCPConnection(this, IP_ADDR, PORT);
        } catch (IOException e) {
            printMsg("Connection Exception: " + e);
        }

        setVisible(true);



    }

    // пишем в текстовое поле
    // будет работать из разных потоков
    private synchronized void printMsg(String msg){
        // так как работать будет из разных потоков
        // в методе invokeLater() создаем экземпляр анонимного класса и реализуем метод runnable()
        // выполняем в потоке окна
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // добавляем строчку которая к нам прилетела
                chatLog.append(msg + "\n");
                // для автоматического автоскролла устанавливаем каретку в самый конец документа
//                chatLog.setCaretPosition(chatLog.getDocument().getLength());

            }
        });
    }

    /**
     * Так как соединение будет одно, синхронизировать методы нет смысла
     *
     */

    // при нажатии кнопку получаем строчку у поля getText()
    @Override
    public void actionPerformed(ActionEvent e) {
        String msg = fieldInputMsg.getText();
        // проверка отправки пустой строчки
        if(msg.equals("")) return;
        // если строчка не пустая, то стираем то, что находится в поле fieldsInputMsg
        fieldInputMsg.setText(null);
        //  в соединение передаем строчку
        connection.sendString(fieldNickName.getText() + ": " + msg);

    }

    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {
        // printMsg() используется из потока TCPConnection
        printMsg("Connection ready...");
    }

    @Override
    public void onReceiveString(TCPConnection tcpConnection, String value) {
        // когда приняли строчкеу - печатаем
//        System.out.println("oNRECEIVE");
        printMsg(value);
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
            printMsg("Connection close");
    }

    @Override
    public void onException(TCPConnection tcpConnection, Exception e) {
        printMsg("Connection Exception: " + e);
    }
}
