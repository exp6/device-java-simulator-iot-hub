package cl.economiza.sensor.simulator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.Gson;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageCallback;

/**
 * Aplicacion que simula el envio de telemetria a la nube Azure Microsoft
 *
 */
public class App 
{
	private static final String connectionString = “<device connection string>”;
	private static IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
	private static final String deviceId = “<device id>”;
	private static DeviceClient client;
	
	public static void main(String[] args) throws IOException, IllegalArgumentException, URISyntaxException {
		client = new DeviceClient(connectionString, protocol);
		
		MessageCallback callback = new AppMessageCallback();
		
		client.setMessageCallback(callback, null);
		client.open();

		MessageSender sender = new MessageSender();

		ExecutorService executor = Executors.newFixedThreadPool(1);
		executor.execute(sender);

		System.out.println("Press ENTER to exit.");
		System.in.read();
		executor.shutdownNow();
		client.close();
	}
	
	private static class EventCallback implements IotHubEventCallback
	{
	  public void execute(IotHubStatusCode status, Object context) {
	    System.out.println("IoT Hub responded to message with status: " + status.name());

	    if (context != null) {
	      synchronized (context) {
	        context.notify();
	      }
	    }
	  }
	}
	
	private static class MessageSender implements Runnable {

		  public void run()  {
			  
			  Double[] o2 = {new Double("6.2"),new Double("7.9"),new Double("9.2"),new Double("5.2"),new Double("4.6")};
			  Integer[] woodTypes = {new Integer("1"),new Integer("2"),new Integer("3")};
			  String clientId = java.util.UUID.randomUUID().toString();
			  
			  int indexes = 0;
			  int totalLength = o2.length;
			  
		    try {
		      
		      while (true) {
		        
		    	if(indexes == totalLength) indexes = 0; 
		    	  
		        TelemetryDataPoint telemetryDataPoint = new TelemetryDataPoint();
		        telemetryDataPoint.deviceId = deviceId;
		        telemetryDataPoint.data = new TelemetryDataPointSub();
		        telemetryDataPoint.data.oxygen = o2[indexes];
		        telemetryDataPoint.data.clientId = clientId;
		        telemetryDataPoint.data.woodType = woodTypes[0];

		        ++indexes;
		        
		        String msgStr = telemetryDataPoint.serialize();
		        Message msg = new Message(msgStr);
		        msg.setMessageId(java.util.UUID.randomUUID().toString()); 
		        System.out.println("Sending: " + msgStr);

		        Object lockobj = new Object();
		        EventCallback callback = new EventCallback();
		        client.sendEventAsync(msg, callback, lockobj);

		        synchronized (lockobj) {
		          lockobj.wait();
		        }
		        Thread.sleep(8000);
		      }
		    } catch (InterruptedException e) {
		      System.out.println("Finished.");
		    }
		  }
		}
	
	private static class TelemetryDataPointSub {
		  public Double oxygen;
		  public String clientId;
		  public Integer woodType;

		  public String serialize() {
		    Gson gson = new Gson();
		    return gson.toJson(this);
		  }
		}

	private static class TelemetryDataPoint {
		  public String deviceId;
		  public TelemetryDataPointSub data;

		  public String serialize() {
		    Gson gson = new Gson();
		    return gson.toJson(this);
		  }	
	}
	
	private static class AppMessageCallback implements MessageCallback {
		
		  public IotHubMessageResult execute(Message msg, Object context) {
			  
		    System.out.println("Received message from hub: "
		      + new String(msg.getBytes(), Message.DEFAULT_IOTHUB_MESSAGE_CHARSET));

		    return IotHubMessageResult.COMPLETE;
		  }
	}
}
