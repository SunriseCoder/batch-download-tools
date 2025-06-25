package app.core;

import java.util.concurrent.Future;

public record TaskWrap<T>(T task, Future<T> future) {}
