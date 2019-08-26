import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioClient {
    private Selector selector;
    private BufferedReader clientInput = new BufferedReader(new InputStreamReader(System.in));
    public void init() throws IOException {
        this.selector = Selector.open(); //选择一个select
        SocketChannel channel = SocketChannel.open(); //创建channel
        channel.configureBlocking(false); //设置channell为nonBlock
        channel.connect(new InetSocketAddress("120.0.0.1",8080)); //设置链接地址
        channel.register(selector, SelectionKey.OP_CONNECT); //注册connect事件
    }

    public void start() throws IOException {
        while (true) {
            selector.select();
            Iterator<SelectionKey> ite = this.selector.selectedKeys().iterator();
            while (ite.hasNext()) {
                SelectionKey selectionKey = ite.next();
                ite.remove();
                if (selectionKey.isConnectable()) {
                    connect(selectionKey);
                } else {
                    read(selectionKey);
                }
            }

        }
    }

    private void connect(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        if (channel.isConnectionPending()) {
            if (channel.finishConnect()) {
                channel.configureBlocking(false);
                channel.register(this.selector, SelectionKey.OP_READ);
                String request = clientInput.readLine();
                channel.write(ByteBuffer.wrap(request.getBytes()));
            }
        } else {
            selectionKey.cancel();
        }
    }

    private void read(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
        channel.read(byteBuffer);
        String response = new String(byteBuffer.array()).trim();
        System.out.println("response is:" + response);
        String nextRequest = clientInput.readLine();
        ByteBuffer outBuffer = ByteBuffer.wrap(nextRequest.getBytes());
        channel.write(outBuffer);

    }
}
