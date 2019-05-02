package fr.adamaq01.ozao.net.server.backend.udp;

import fr.adamaq01.ozao.net.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;

class UDPChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private UDPServerBackend serverBackend;
    private UDPConnection connection;

    protected UDPChannelHandler(UDPServerBackend serverBackend, UDPConnection connection) {
        this.serverBackend = serverBackend;
        this.connection = connection;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.serverBackend.handlers.forEach(handler -> handler.onConnect(connection));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        this.serverBackend.handlers.forEach(handler -> handler.onDisconnect(connection));
        serverBackend.connections.remove(connection);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        Packet packet = Packet.create(ReferenceCountUtil.retain(msg));
        this.serverBackend.handlers.forEach(handler -> handler.onPacketReceive(connection, packet));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
    }
}