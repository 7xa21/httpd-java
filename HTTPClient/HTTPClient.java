/*
 * HTTPClient.java - a simple HTTP request client.
 * 
 * Randy Sorensen <sorensra@msudenver.edu>
 * Homework 3 (CS3700 - Computer Networks)
 * 2013-Feb-12
 * 
 * Notes:
 *  This program was tested with OpenJDK 7 on Linux Mint 14.
 *  Linux Mint is a popular Ubuntu Linux derivative.
 */
import java.io.*;
import java.net.*;


/**
 * A very basic HTTP client.
 * 
 * @author Randy Sorensen
 */
public class HTTPClient {

    // Member variables.
    private int m_port = 8080;
    private BufferedReader m_consoleReader;

    private String m_remoteHost;
    private String m_httpMethod;
    private String m_httpVersion;
    private String m_resourceName;
    private String m_userAgent;
        
    private Socket m_tcpSocket = null;
    private BufferedReader m_inReader = null;
    private PrintWriter m_outWriter = null;
    
    /**
     * Reads the session parameters from the user on the console.
     */
    public void readClientParams() throws IOException {
        // Get remote host name or address
        System.out.print("Enter host name or IP address: ");
        m_remoteHost = m_consoleReader.readLine();

        // Get HTTP method
        System.out.print("Enter HTTP method type (default is \"GET\"): ");
        m_httpMethod = m_consoleReader.readLine();
        if (m_httpMethod.equals(""))
            m_httpMethod = "GET";
        
        // Get HTTP version
        System.out.print("Enter HTTP version (default is \"1.1\"): ");
        m_httpVersion = m_consoleReader.readLine();
        if (m_httpVersion.equals(""))
            m_httpVersion = "1.1";
        
        // Get resource (e.g. file) name
        System.out.print("Enter resource name: http://"
               + m_remoteHost + ":" + m_port + "/");
        m_resourceName = m_consoleReader.readLine();
        
        // Get desired User-Agent string
        System.out.print("Enter User-Agent string"
                    + " (default is \"HW3 HTTPClient\"): ");
        m_userAgent = m_consoleReader.readLine();
        if (m_userAgent.equals(""))
            m_userAgent = "HW3 HTTPClient";
    }

    /**
     * Initialize the socket and its input / output streams.
     * 
     * Session parameters should have already been initialized,
     * e.g. via readClientParams().
     */
    
    public boolean initSocket() throws IOException {
        try {
            m_tcpSocket = new Socket(m_remoteHost, m_port);
        } catch (UnknownHostException e) {
            System.err.print("Unable to resolve host: " + m_remoteHost);
            return false;
        }
        
        m_inReader = new BufferedReader(
                new InputStreamReader(m_tcpSocket.getInputStream()));
        m_outWriter = new PrintWriter(m_tcpSocket.getOutputStream(), true);
        
        return true;
    }

    /**
     * Closes the session socket and its related streams.
     */
    public void closeSocket() throws IOException {
        m_outWriter.close();
        m_inReader.close();
        m_tcpSocket.close();
    }

    /**
     * Writes the HTTP request header to the socket.
     * 
     * Uses information collected from readClientParams() and the
     * socket initialized with initSocket().
     */
    public void writeRequestHeader() {
        m_outWriter.println(m_httpMethod + " /" + m_resourceName
                + " " + "HTTP/" + m_httpVersion);
        m_outWriter.println("Host: " + m_remoteHost);
        m_outWriter.println("User-Agent: " + m_userAgent);
        
        m_outWriter.println("");  // blank line terminates request header
    }

    /**
     * Reads A) the server's response header and B) the response
     * content, if any.
     * 
     * The response header is displayed on the console.
     * 
     * The response content is saved to a local file with the
     * same name as the resource name on the server.
     */
    public void readResponse() throws IOException {
        // Read response header
        String responseLine;
        int contentLength = 0;
        while ((responseLine = m_inReader.readLine()) != null) {
            System.out.println(responseLine);
            if (responseLine.startsWith("Content-Length: ")) {
                responseLine =
                        responseLine.substring("Content-Length: ".length());
                contentLength = Integer.parseInt(responseLine);
            }
            else if (responseLine.equals(""))
                break;
        }
        
        // Read response content from socket, if any.
        // Dump contents to file.
        if (contentLength > 0) {
            char recvBytes[] = new char[contentLength];
            m_inReader.read(recvBytes);

            // Use only the resource name (with no path).
            // Note that his will break for root resource
            // requests (e.g. "GET /" with no path info)
            String fileName = m_resourceName;
            if (fileName.indexOf('/') >= 0) {
                fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                if (fileName.length() == 0)
                    throw new IOException("Invalid resource name.");
            }
            PrintWriter fileWriter = new PrintWriter(fileName, "UTF-8");
            fileWriter.print(recvBytes);
            fileWriter.close();
        }
    }

    /**
     * Runs one full HTTP session.
     * 
     * Asks the client to enter HTTP session parameters, then
     * executes the request.
     */
    public void newSession() throws IOException {
        readClientParams();
        if (!initSocket())
            return;

        writeRequestHeader();
        readResponse();
        
        closeSocket();
    }

    /**
     * Runs the client until the user declines to continue.
     */
    public void run() throws IOException {
        m_consoleReader = new BufferedReader(
                new InputStreamReader(System.in));
        
        // Keep running sessions until the user enters something
        // starting with 'n'.
        String cont = "";
        do {
            newSession();
            System.out.print("Run another session (y/n)? ");
            cont = m_consoleReader.readLine();
        } while (cont.toLowerCase().charAt(0) != 'n');
        
        m_consoleReader.close();
    }

    /**
     * Program entry.
     * 
     * Just a shell that instantiates the HTTPClient class, calls
     * its run() method, and displays unhandled exceptions.
     */
    public static void main(String[] args) throws IOException {
        try {
            HTTPClient client = new HTTPClient();
            client.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
