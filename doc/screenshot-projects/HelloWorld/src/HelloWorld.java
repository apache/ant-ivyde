import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HelloWorld {

	public static void main(String[] args) {
		Log log = LogFactory.getLog(HelloWorld.class);
		log.info("Hello world");
	}

}
