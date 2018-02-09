/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;



public class WebWorker implements Runnable
{

//added url variable
String url;
private Socket socket;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      
      //modified readHTTPRequest to return a string
      url = readHTTPRequest(is);
      
      //send different strings as content type 
      if (url.contains(".html"))
         writeHTTPHeader(os, "text/html");
      
      else if (url.contains(".gif"))
         writeHTTPHeader(os, "image/gif");
      
      else if (url.contains(".jpg"))
         writeHTTPHeader(os, "image/jpeg");
      
      else if (url.contains(".png"))
         writeHTTPHeader(os, "image/png");
         
      //modified writeContent to accept a string of the filepath
      writeContent(os, url);
      
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private String readHTTPRequest(InputStream is)
{
   
   //added variables
   String headerArray[] = new String[10];
   String fileHeader = "";
   
   String line;
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
      
         System.err.println("Request line: ("+line+")");
         if (line.length()==0) break;
         
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
      
      //if it contains a GET and not favicon, split line into words
      //and store in headerArray. the word we want is headerArray[1]
         if (line.contains("GET") && !(line.contains("favicon"))) {
            
            headerArray = line.split("\\s+");
            fileHeader = headerArray[1];
            
         }//end if
      
      //debugging
      //System.out.println("fileHeader is: " + fileHeader);
   }
   
   return fileHeader;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   os.write("HTTP/1.1 200 OK\n".getBytes());
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Jon's very own server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os, String filePath) throws Exception
{

   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   
   String s;
   filePath = (System.getProperty("user.dir") + filePath);
   File inputFile = new File(filePath);
   
   //if html
   if (filePath.contains(".html")) {
   
      os.write("<html><head></head><body>\n".getBytes());
      os.write("<h3>My web server works!</h3>\n".getBytes());
   
      try {
      
         Scanner fileScan = new Scanner(inputFile);
      
         while (fileScan.hasNext()) {
      
            s = fileScan.nextLine();
            
         
            if (s.contains("<cs371date>"))
               os.write(s.replace("<cs371date>",(df.format(d))).getBytes());
            else if (s.contains("<cs371server>"))
               os.write(s.replace("<cs371server>", "This is Lucas's server").getBytes());
            else
               os.write(s.getBytes());
      
         os.write("</br>".getBytes());
         
         }//end while
      }//end try
   
      catch (FileNotFoundException e) {
         os.write("404 Not Found.".getBytes());
         
      }//end catch
      os.write("</body></html>\n".getBytes());
   
   }//end if html
   
   
   //if png, jpg or gif
   else if (filePath.contains(".png") || filePath.contains(".jpg") || filePath.contains(".gif")) {
      try (InputStream inputStream = new FileInputStream(inputFile);){
      
         double fileSize = inputFile.length();
      
         byte[] byteArray = new byte[(int) fileSize];
      
         inputStream.read(byteArray);
         os.write(byteArray);
         
      }//end try
      
      catch (FileNotFoundException e) {
         
         os.write("404 Not Found.".getBytes());
      }//end catch
      
   }//end else
      
} //end writeContent

} // end class
