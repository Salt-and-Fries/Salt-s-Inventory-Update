
    // Excluded network fixture: delay the real local connection's outbound
    // packets in FIFO order, through Netty's ordinary event loop.
    static final QaPacketDelay qaPacketDelay=new QaPacketDelay();
    static final class QaPacketDelay extends io.netty.channel.ChannelDuplexHandler {
        volatile int millis,jitter;
        final java.util.concurrent.atomic.AtomicInteger pending=new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicLong submitted=new java.util.concurrent.atomic.AtomicLong();
        final java.util.concurrent.atomic.AtomicLong delivered=new java.util.concurrent.atomic.AtomicLong();
        long lastDue;
        final java.util.ArrayDeque<Delayed> packets=new java.util.ArrayDeque<>();
        static final class Delayed {final Object message;final io.netty.channel.ChannelPromise promise;boolean ready;
            Delayed(Object message,io.netty.channel.ChannelPromise promise){this.message=message;this.promise=promise;}}
        public void write(io.netty.channel.ChannelHandlerContext ctx,Object message,io.netty.channel.ChannelPromise promise){
            long ordinal=submitted.getAndIncrement();
            long delay=Math.max(0,millis+(jitter==0?0:(ordinal%3-1)*jitter));
            if(delay==0&&pending.get()==0){ctx.write(message,promise);delivered.incrementAndGet();return;}
            long now=System.nanoTime(),due=Math.max(now+delay*1_000_000L,lastDue+1);lastDue=due;pending.incrementAndGet();
            Delayed packet=new Delayed(message,promise);packets.addLast(packet);
            ctx.executor().schedule(()->{
                packet.ready=true;
                // Timer invocation overhead can reverse near-equal deadlines.
                // Readiness may arrive out of order; native writes must not.
                while(!packets.isEmpty()&&packets.peekFirst().ready){
                    Delayed next=packets.removeFirst();
                    try{
                        if(ctx.channel().isActive())ctx.writeAndFlush(next.message,next.promise);
                        else {io.netty.util.ReferenceCountUtil.release(next.message);next.promise.tryFailure(new IllegalStateException("QA connection closed before delayed packet delivery"));}
                    }finally{pending.decrementAndGet();delivered.incrementAndGet();}
                }
            },Math.max(0,due-now),java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }
    static void qaNetworkDelay(JsonObject c){
        int delay=num(c,"millis",0),jitter=num(c,"jitter",0);
        if(delay<0||delay>200||jitter<0||jitter>100)throw new IllegalArgumentException("Bounded QA latency is 0..200ms plus 0..100ms jitter");
        Object connection=mc.getConnection().getConnection();io.netty.channel.Channel channel=null;
        for(Class<?> type=connection.getClass();type!=null;type=type.getSuperclass())for(var f:type.getDeclaredFields())if(io.netty.channel.Channel.class.isAssignableFrom(f.getType())){
            try{f.setAccessible(true);channel=(io.netty.channel.Channel)f.get(connection);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        if(channel==null)throw new IllegalStateException("Native connection has no live Netty channel");
        io.netty.channel.Channel live=channel;
        live.eventLoop().submit(()->{
            if(live.pipeline().get("siu_qa_packet_delay")==null)live.pipeline().addBefore("packet_handler","siu_qa_packet_delay",qaPacketDelay);
            if(qaPacketDelay.pending.get()!=0)throw new IllegalStateException("Do not reconfigure a nonempty delay queue");
            qaPacketDelay.millis=delay;qaPacketDelay.jitter=jitter;
        }).syncUninterruptibly();
        write("regression-network-delay.json",Map.of("millis",delay,"jitter",jitter,"scope","Actual native client outbound packets, FIFO Netty scheduling. No packet contents, server tick rate or UI logic changed."));
    }
    static void qaNetworkStatus(){
        Map<?,?> probe;
        try{probe=(Map<?,?>)Class.forName("siu.qa.clickprobe.UiClickProbeRuntime",true,null).getMethod("fence").invoke(null);}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Latency tests require the excluded click probe",error);}
        write("regression-network-status.json",Map.of("outboundPending",qaPacketDelay.pending.get(),"outboundSubmitted",qaPacketDelay.submitted.get(),"outboundDelivered",qaPacketDelay.delivered.get(),
            "clicks",probe,"pendingGesture",com.salts_inventory_update.client.DesktopContainerClient.hasPendingSlotMutations()));
    }
