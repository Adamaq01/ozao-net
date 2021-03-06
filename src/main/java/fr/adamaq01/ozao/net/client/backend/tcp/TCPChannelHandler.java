package fr.adamaq01.ozao.net.client.backend.tcp;

import fr.adamaq01.ozao.net.Buffer;
import fr.adamaq01.ozao.net.OzaoException;
import fr.adamaq01.ozao.net.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

class TCPChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private TCPClient client;

    protected TCPChannelHandler(TCPClient client) {
        this.client = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.client.getHandlers().forEach(handler -> handler.onConnect(client));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        this.client.getHandlers().forEach(handler -> handler.onDisconnect(client));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        Buffer buffer = Buffer.create(ReferenceCountUtil.retain(msg));
        this.client.getProtocol().cut(buffer).stream().filter(data -> {
            if (!this.client.getProtocol().verify(data)) {
                exceptionCaught(ctx, new OzaoException("Received a packet that does not suit the protocol requirements !"));
                return false;
            }
            return true;
        }).map(data -> this.client.getProtocol().decode(data)).forEachOrdered(packet -> {
            this.client.getHandlers().forEach(handler -> handler.onPacketReceive(client, packet));
            this.client.getPacketHandlers().stream().filter(packetHandler -> packetHandler.verify(packet)).forEach(handler -> handler.onPacketReceive(client, packet));
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        this.client.getHandlers().forEach(handler -> handler.onException(client, cause));
    }
}
