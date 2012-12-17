package momu.jms;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface ResponseVerifier {

    void verify(String message);

    static class Result<T> {

        private final T actual;
        private final AssertionError failure;
        private final Exception error;

        public static <T> Result<T> success(T actual) {
            return new Result<T>(actual);
        }

        public static <T> Result<T> fail(AssertionError a) {
            return new Result<T>(a);
        }

        public static <T> Result<T> error(Exception e) {
            return new Result<T>(e);
        }

        public Result(T actual) {
            this.actual = actual;
            failure = null;
            error = null;
        }

        public Result(AssertionError failure) {
            actual = null;
            this.failure = failure;
            error = null;
        }

        public T getActual() {
            return actual;
        }

        public Result(Exception error) {
            actual = null;
            failure = null;

            this.error = error;
        }

        public void assertSuccess() throws Exception {
            if (failure != null) {
                throw failure;
            }
            if (error != null) {
                throw error;
            }
        }
    }

    public class Results<T> {
        private final List<Result<T>> results = new CopyOnWriteArrayList<Result<T>>();
        private final CountDownLatch responseCountDown;

        public Results(int expectedResultCount) {
            responseCountDown = new CountDownLatch(expectedResultCount);
        }

        public void error(Exception e) {
            results.add(Result.<T>error(e));
            responseCountDown.countDown();
        }

        public void fail(AssertionError a) {
            results.add(Result.<T>fail(a));
            responseCountDown.countDown();
        }

        public void success(T actual) {
            results.add(Result.<T>success(actual));
            responseCountDown.countDown();
        }

        public void await() throws InterruptedException, TimeoutException {
            if (!responseCountDown.await(1, TimeUnit.SECONDS)) {
                throw new TimeoutException();
            }
        }

        public void assertSuccess() throws Exception {
            for (Result<T> result : results) {
                result.assertSuccess();
            }
        }
    }
}
