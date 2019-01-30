package com.ten.ware.vesta.service.impl.populater;

import com.ten.ware.vesta.service.bean.Id;
import com.ten.ware.vesta.service.impl.bean.IdMeta;
import com.ten.ware.vesta.service.impl.timer.Timer;

import java.util.concurrent.atomic.AtomicReference;

/**
 * CAS无锁版本
 */
public class AtomicIdPopulator implements IdPopulator, ResetPopulator {

    /**
     * 联合数据结构
     */
    class Variant {

        private long sequence = 0;
        private long lastTimestamp = -1;

    }

    /**
     * 原子变量引用，保证联合数据结构中任意一个被修改，都可以安全更新
     */
    private AtomicReference<Variant> variant = new AtomicReference<Variant>(new Variant());

    public AtomicIdPopulator() {
        super();
    }

    public void populateId(Timer timer, Id id, IdMeta idMeta) {
        Variant varOld, varNew;
        long timestamp, sequence;

        // CAS大循环
        while (true) {

            // Save the old variant
            varOld = variant.get();

            // populate the current variant
            timestamp = timer.genTime();
            timer.validateTimestamp(varOld.lastTimestamp, timestamp);

            sequence = varOld.sequence;

            if (timestamp == varOld.lastTimestamp) {
                sequence++;
                sequence &= idMeta.getSeqBitsMask();
                if (sequence == 0) {
                    timestamp = timer.tillNextTimeUnit(varOld.lastTimestamp);
                }
            } else {
                sequence = 0;
            }

            // Assign the current variant by the atomic tools
            varNew = new Variant();
            varNew.sequence = sequence;
            varNew.lastTimestamp = timestamp;

            // CAS 若相同，则跳出循环
            if (variant.compareAndSet(varOld, varNew)) {
                id.setSeq(sequence);
                id.setTime(timestamp);

                break;
            }

        }
    }

    public void reset() {
        variant = new AtomicReference<Variant>(new Variant());
    }

}
