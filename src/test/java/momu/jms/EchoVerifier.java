package momu.jms;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class EchoVerifier implements ResponseVerifier {
    private final String echo;

    public EchoVerifier(String echo) {
        this.echo = echo;
    }

    @Override
    public void verify(String message) {
        assertThat(message, is(equalTo(echo + " and thanks")));
    }
}
