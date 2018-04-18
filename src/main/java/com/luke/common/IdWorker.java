package com.luke.common;

public class IdWorker {

    //机器编号
    private long workerId;

    //时间起始标记点，作为基准，一般取系统的最近时间
    private final long start = 1523947963797L;

    //机器标识位数
    private long workerIdBits = 10L;

    //毫秒内自增位
    private final long sequenceBits = 12L;

    // 4095,111111111111,12位
    private final long sequenceMask = 1L << this.sequenceBits - 1L;

    // 0，并发控制
    private long sequence = 0L;

    // 22
    private final long timestampLeftShift = this.sequenceBits + this.workerIdBits;

    // 12
    private final long workerIdShift = this.sequenceBits;

    // 机器ID最大值: 1023
    private long maxWorkerId = 1L << this.workerIdBits - 1L;

    private long lastTimestamp = -1L;

    private static IdWorker flowIdWorker = new IdWorker(1);

    public static IdWorker getFlowIdWorkerInstance() {
        return flowIdWorker;
    }

    private IdWorker(long workerId) {
        if (workerId > this.maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException("workerId不合法");
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() throws Exception {
        long timestamp = this.timeGen();
        if (this.lastTimestamp == timestamp) { // 如果上一个timestamp与新产生的相等，则sequence加一(0-4095循环); 对新的timestamp，sequence从0开始
            this.sequence = this.sequence + 1 & this.sequenceMask;
            if (this.sequence == 0) {
                timestamp = this.tilNextMillis(this.lastTimestamp);// 重新生成timestamp
            }
        } else {
            this.sequence = 0;
        }

        if (timestamp < this.lastTimestamp) {
            System.err.println("时钟向后转动,暂时不能生成id,请等待");
            throw new Exception("时钟向后转动,暂时不能生成id,请等待");
        }

        this.lastTimestamp = timestamp;
        return (timestamp - this.start) << this.timestampLeftShift | this.workerId << this.workerIdShift | this.sequence;
    }

    /**
     * 获得系统当前毫秒数
     */
    private static long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 等待下一个毫秒的到来, 保证返回的毫秒数在参数lastTimestamp之后
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = this.timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = this.timeGen();
        }
        return timestamp;
    }

    public static void main(String[] args) throws Exception {
        IdWorker idWorker = IdWorker.getFlowIdWorkerInstance();
        // System.out.println(Long.toBinaryString(idWorker.nextId()));
        System.out.println(idWorker.nextId());
        System.out.println(idWorker.nextId());
    }


}
