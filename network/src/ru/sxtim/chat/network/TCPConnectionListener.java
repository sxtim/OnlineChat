package ru.sxtim.chat.network;

/** Описываем события TCPConnection
 *
 */
public interface TCPConnectionListener {
    // Готовое соединение, мы с ним можем работать
    // будем туда передавать экземпляр самого соединения для того чтобы
    // у той сущности которая реализует интерфейс был доступ к источнику этого события
    void onConnectionReady(TCPConnection tcpConnection);

    // Соединение приняло строчку. Смотрим в обработчике, что за строчку мы приняли
    void onReceiveString(TCPConnection tcpConnection, String value);

    // Disconnect - соединение порвалось. Это событие тоже может быть интересно снаружи
    void onDisconnect(TCPConnection tcpConnection);

    // Исключение - что-то пошло не так. Помимо самого источника события передаем объект исключения
    void onException(TCPConnection tcpConnection, Exception e);
}
