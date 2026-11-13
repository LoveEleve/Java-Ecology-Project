package com.loveeleve.ecology.jraft.service;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import com.loveeleve.ecology.jraft.model.KvCommand;

import java.util.concurrent.CompletableFuture;

public class KvClosure implements Closure {

    private final KvCommand command;
    private final CompletableFuture<String> result = new CompletableFuture<>();

    public KvClosure(KvCommand command) {
        this.command = command;
    }

    public KvCommand command() {
        return command;
    }

    public CompletableFuture<String> result() {
        return result;
    }

    public void complete(String value) {
        result.complete(value);
    }

    @Override
    public void run(Status status) {
        if (!status.isOk() && !result.isDone()) {
            result.completeExceptionally(new IllegalStateException(status.getErrorMsg()));
        }
    }
}
