package cn.com.sherhom.reno.kafka.common.record;

import cn.com.sherhom.reno.common.utils.CsvWriter;
import cn.com.sherhom.reno.common.utils.DateUtil;
import cn.com.sherhom.reno.common.utils.FileUtil;
import cn.com.sherhom.reno.kafka.common.holder.ListCSVHolder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;

/**
 * @author Sherhom
 * @date 2020/9/8 10:14
 */
@Slf4j
public class Stat {

    private final StampedLock stampedLock = new StampedLock();
    private volatile boolean failed;
    private long startTime;
    private long endTime;
    private volatile boolean started=false;
    private AtomicLong totalBytes;
    private AtomicLong totalLatency;
    private AtomicLong msg;
    private AtomicLong maxLatency;

    private boolean isWriteToFile;
    private long windowStart;
    private AtomicLong windowCount;
    private long windowMaxLatency;
    private AtomicLong windowTotalLatency;
    private AtomicLong windowBytes;
    private long reportingInterval;
    private ConcurrentLinkedDeque<Integer> indexList;
    private CsvWriter csvWriter;
    private CountDownLatch countDownLatch;

    private String filePath;
    public Stat(long totalBytes, long msg, long reportingInterval, int threadNum) {
        this.startTime =System.currentTimeMillis();
        this.failed =false;
        this.totalBytes =new AtomicLong(totalBytes);
        this.reportingInterval=reportingInterval;
        this.totalLatency =new AtomicLong(0);
        this.msg =new AtomicLong(msg);

        this.maxLatency =new AtomicLong(0);
        this.indexList=new ConcurrentLinkedDeque();
        this.newWindows();
        this.isWriteToFile = false;
        countDownLatch=new CountDownLatch(threadNum);
    }
    public Stat(long totalBytes, long msg, long reportingInterval, int threadNum,boolean isWriteToFile,String path) {
        this.startTime =System.currentTimeMillis();
        this.failed =false;
        this.totalBytes =new AtomicLong(totalBytes);
        this.reportingInterval=reportingInterval;
        this.totalLatency =new AtomicLong(0);
        this.msg =new AtomicLong(msg);

        this.maxLatency =new AtomicLong(0);
        this.indexList=new ConcurrentLinkedDeque();
        this.newWindows();
        this.isWriteToFile = isWriteToFile;
        countDownLatch=new CountDownLatch(threadNum);
        String fileName="reno_kfk_producer_"+ DateUtil.date2String(new Date()) +".csv";
        this.filePath= FileUtil.getPathAndFile(path,fileName);
        this.csvWriter=new CsvWriter(this.filePath, ListCSVHolder.metricCsvLine);
    }
    public void start(){
        countDownLatch.countDown();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(!started){
            synchronized (countDownLatch){
                if(!started){
                    this.startTime =System.currentTimeMillis();
                    started=true;
                }
            }
        }
        log.info("Thread:{} started stat",Thread.currentThread().getName());
    }
    public void stop(){
        this.started=false;
        this.endTime=System.currentTimeMillis();
    }
    public boolean isFailed() {
        return failed;
    }
    public void setFailed() {
        this.failed = true;
    }
    public long getBytePerSec(){
        long ellapsed = getElapse();
        return ellapsed==0?0:(long)(this.totalBytes.longValue()/(ellapsed/1000.0));
    }

    public void record(int index,long bytes,int msg,long latency){
        long stamp=stampedLock.readLock();
        try{
            this.totalBytes.addAndGet(bytes);
            this.msg.addAndGet(msg);
            this.totalLatency.addAndGet(latency);
            this.maxLatency.set(Math.max(latency,this.maxLatency.longValue()));
            this.indexList.add(index);

            this.windowCount.incrementAndGet();
            this.windowMaxLatency=Math.max(latency,windowMaxLatency);
            this.windowTotalLatency.addAndGet(latency);
            this.windowBytes.addAndGet(bytes);
        } finally {
            stampedLock.unlockRead(stamp);
        }
        if(this.reportingInterval>0&&System.currentTimeMillis()-this.windowStart>this.reportingInterval){
            stamp=stampedLock.writeLock();
            try {
                if(this.reportingInterval>0&&System.currentTimeMillis()-this.windowStart>this.reportingInterval)
                    printAndNewWindow();
            } finally {
                stampedLock.unlockWrite(stamp);
            }
        }
    }

    public AtomicLong getTotalBytes() {
        return totalBytes;
    }

    public AtomicLong getTotalLatency() {
        return totalLatency;
    }

    public AtomicLong getMsg() {
        return msg;
    }

    public AtomicLong getWindowCount() {
        return windowCount;
    }

    public void newWindows(){
        this.windowStart = System.currentTimeMillis();
        this.windowCount = new AtomicLong(0);
        this.windowMaxLatency = 0;
        this.windowTotalLatency =  new AtomicLong(0);
        this.windowBytes = new AtomicLong(0);
    }

    private void printAndNewWindow(){
        if(this.isWriteToFile){
            writeWindowsToFile();
        }
        printWindows();
        newWindows();
    }

    private static final String winFormat=
            "win_max_latency(ms):%s " +
            "win_total_byte:%s " +
            "win_byte/s:%s " +
            "win_total_msg:%s " +
            "win_msg/s:%s " +
            "win_total_latency(ms):%s";
    private void printWindows(){;
        log.info(windowsMgs());
    }
    private void writeWindowsToFile(){
        writeToFile(windowsMetric());
    }
    private String windowsMgs(){
        Metric metric = windowsMetric();
        return String.format(winFormat,
                metric.getMaxLatency(),
                metric.getTotalBytes(),
                metric.getBytePerSec(),
                metric.getTotalCount(),
                metric.getCountPerSec(),
                metric.getTotalLatency());
    }
    private Metric windowsMetric(){
        long ellapsed = getWindowsElapse();
        return new Metric(windowMaxLatency,
                windowBytes.longValue(),
                (long)(ellapsed>0?windowBytes.longValue()/(ellapsed/1000.0):0),
                windowCount.intValue(),
                (long)(ellapsed>0?windowCount.intValue()/(ellapsed/1000.0):0),
                windowTotalLatency.longValue());
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class Metric{
        long maxLatency;
        long totalBytes;
        long bytePerSec;
        long totalCount;
        long countPerSec;
        long totalLatency;
    }
    private static final String sFormat=
            "maxLatency(ms):%s " +
            "total_byte:%s " +
            "byte/s:%s " +
            "total_msg:%s " +
            "msg/s:%s " +
            "total_latency(ms):%s";
    public void printTotal(){
       log.info(totalMsg());
       if(canWrite())
        writeTotalToFile();
    }
    public void writeTotalToFile(){
        writeToFile(totalMetric());
    }
    public void closeFile(){
        if(csvWriter.isOpened())
            csvWriter.close();
    }
    public String totalMsg(){
        long stamp=stampedLock.writeLock();
        long ellapsed = getElapse();
        String res;
        try {
            //windowCount:%s latency(ms):%s maxLatency(ms):%s byte/s:%s msg/s:%s
            res=String.format(sFormat,
                    maxLatency.longValue(),
                    totalBytes.longValue(),
                    totalBytes.longValue()/(ellapsed/1000.0),
                    msg.intValue(),
                    msg.intValue()/(ellapsed/1000.0),
                    totalLatency);
        }finally {
            stampedLock.unlockWrite(stamp);
        }
        return res;
    }
    public Metric totalMetric(){
        long stamp=stampedLock.writeLock();
        long ellapsed = System.currentTimeMillis() - startTime;
        Metric res;
        try {
            //windowCount:%s latency(ms):%s maxLatency(ms):%s byte/s:%s msg/s:%s
            res=new Metric(
                    maxLatency.longValue(),
                    totalBytes.longValue(),
                    (long)(totalBytes.longValue()/(ellapsed/1000.0)),
                    msg.intValue(),
                    (long)(msg.intValue()/(ellapsed/1000.0)),
                    totalLatency.longValue());
        }finally {
            stampedLock.unlockWrite(stamp);
        }
        return res;
    }
    private void writeToFile(Metric metric){
        if(!csvWriter.isOpened())
            csvWriter.open();
        csvWriter.writeLine(metric);
    }
    private boolean canWrite(){
        return csvWriter!=null&&this.isWriteToFile;
    }
    public void writeHeaderToFile(){
        if(canWrite()){
            if(!csvWriter.isOpened())
                csvWriter.open();
            csvWriter.writeHeader();
        }
    }
    public long getElapse(){
        return this.started?System.currentTimeMillis()-this.startTime:this.endTime-this.startTime;
    }
    private long getWindowsElapse(){
        return System.currentTimeMillis()-this.windowStart;
    }
    public String getFilePath() {
        return filePath;
    }
}
