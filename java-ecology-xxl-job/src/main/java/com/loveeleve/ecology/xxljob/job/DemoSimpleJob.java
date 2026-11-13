package com.loveeleve.ecology.xxljob.job;

import com.loveeleve.ecology.xxljob.model.JobResult;
import com.loveeleve.ecology.xxljob.service.DemoJobService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class DemoSimpleJob {

    private final DemoJobService jobService;

    public DemoSimpleJob(DemoJobService jobService) {
        this.jobService = jobService;
    }

    @XxlJob("demoSimpleJob")
    public void execute() {
        String parameter = XxlJobHelper.getJobParam();
        XxlJobHelper.log("demoSimpleJob start, param={}", parameter);
        JobResult result = jobService.simple(parameter);
        XxlJobHelper.log("demoSimpleJob result={}", result);
        if (result.success()) {
            XxlJobHelper.handleSuccess(result.message());
        } else {
            XxlJobHelper.handleFail(result.message());
        }
    }
}
