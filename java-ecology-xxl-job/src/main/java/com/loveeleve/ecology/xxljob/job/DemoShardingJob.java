package com.loveeleve.ecology.xxljob.job;

import com.loveeleve.ecology.xxljob.model.JobResult;
import com.loveeleve.ecology.xxljob.service.DemoJobService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class DemoShardingJob {

    private final DemoJobService jobService;

    public DemoShardingJob(DemoJobService jobService) {
        this.jobService = jobService;
    }

    @XxlJob("demoShardingJob")
    public void execute() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();
        XxlJobHelper.log("demoShardingJob shard={}/{}", shardIndex, shardTotal);
        JobResult result = jobService.sharding(shardIndex, shardTotal, 100);
        XxlJobHelper.log("demoShardingJob result={}", result);
        if (result.success()) {
            XxlJobHelper.handleSuccess(result.message());
        } else {
            XxlJobHelper.handleFail(result.message());
        }
    }
}
