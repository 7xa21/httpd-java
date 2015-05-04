/*
 * HTTPServerSession.java - handles an HTTP request in a Thread.
 * 
 * Randy Sorensen <sorensra@msudenver.edu>
 * Homework 3 (CS3700 - Computer Networks)
 * 2013-Feb-12
 * 
 * 
 * Notes:
 *   After a connection to a client is established, this class is
 *   initialized with the socket returned by
 *   ServerSocket.accept().
 * 
 *   This class is a Thread descendant; the request is read from
 *   the client and handled when the thread is started with
 *   Thread.start().
 * 
 *   If the session receives a request containing "exit", "null"
 *   or simply "", System.exit(0) is called, killing the entire
 *   server (all sibling threads included).
 */
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;


/**
 * A single HTTP session in a discrete thread.
 * 
 * The session's socket is supplied to the constructor along with
 * the server's document root path.
 * 
 * @author Randy Sorensen
 */
public class HTTPServerSession extends Thread {

    // Member variables.
    private boolean m_showClientHeader;     // On console
    private Socket m_socket;                // Session socket.
    private String m_docPath;               // Document root path.
    private OutputStream m_outStream;       // For .write(byte[]).
    private PrintWriter m_outWriter;        // For .println(String).

     
    /**
     * Constructs a new HTTP session using a m_socket that's
     * connected to a client.
     *
     * @param showClientHeader if 'true', show all client
     *                         request headers on the console
     * @param socket the m_socket instance, returned by
     *               ServerSocket.accept()
     * @param docPath the path of the folder to use as the HTTP
     *                server root
     */

    public HTTPServerSession(boolean showClientHeader, Socket socket,
                             String docPath)
            throws IOException
    {
        m_showClientHeader = showClientHeader;
        m_socket = socket;
        m_docPath = docPath;

        m_outStream = socket.getOutputStream();
        m_outWriter = new PrintWriter(this.m_outStream, true);
    }

    /**
     * Sends a proper "HTTP/1.0 ..." header string to the client.
     *
     * Pass 0 in 'contentLength' to omit that field from the
     * header.
     *
     * @param status HTTP response status, e.g. "200 OK"
     * @param contentLength value of the "Content-Length: "
     *                      field, or 0 to skip sending the field
     *                      altogether
     */
    public void sendHeader(String status, long contentLength) {
        m_outWriter.println("HTTP/1.0 " + status);
        m_outWriter.println("Date: " + DateFormat
                .getDateTimeInstance()
                .format(new Date()));
        m_outWriter.println("Server: http://"
                + m_socket.getLocalAddress() + ":"
                + m_socket.getLocalPort() + "/");
        if (contentLength > 0)
            m_outWriter.println("Content-Length: " + contentLength);
        m_outWriter.println("");   // (newline)
    }

    /**
     * Sends a response to the client.
     *
     * If the 'content' is null, which would be expected in the
     * case of an invalid request, this method will wrap the
     * status line (such as "404 Not Found") into a small HTML
     * block and send it as the content body.
     *
     * This way a client browser can see the error message if
     * their request was bad.
     *
     * @param status HTTP response status, e.g. "200 OK"; sent
     *               in first line of response header
     * @param content the response body if the request is valid
     *                and successful, or 'null' as described above
     */
    public void sendResponse(String status, byte[] content)
            throws IOException
    {
        // If response == null, we send the status line (e.g.
        // "404 Not Found") inside a small HTML block as the
        // message content so that the error will be shown in
        // a web browser client.
        if (content == null) {
            // assert(!responseStatus.equals("200 OK"));
            content = (
                    "<html><head><title>" + status + "</title></head>"
                        + "<body><h1>" + status + "</h1></body></html>"
            ).getBytes();
        }
          
        // Full HTTP request reply: send the header followed by
        // the full response body.
        sendHeader(status, content.length);
        m_outStream.write(content);
    }

    /**
     * Overloaded wrapper for responseStatus that passes null in
     * the response parameter.
     *
     * Use this for sending responses other than "200 OK"; the
     * response status text will appear in the client's browser
     * to inform them of the error.
     *
     * @param status HTTP response status, e.g. "404 Not Found"
     */
     
    public void sendResponse(String status) throws IOException {
        sendResponse(status, null);
    }

    /**
     * Handles an HTTP "GET" request.
     *
     * @param urlPath the path from the URL that was specified by
     *                the client
     */
    public void handleGETRequest(String  urlPath) throws IOException {
        // Ensure the leading path separator was provided.
        if (urlPath.charAt(0) != '/') {
            sendResponse("400 Bad Request");
            return;
        }
          
        // Get a 'Path' interface; prepend server's document root path.
        Path docRoot = Paths.get(m_docPath).toAbsolutePath().normalize();
        Path resource = Paths.get(m_docPath + urlPath).toAbsolutePath()
                                                            .normalize();

        // Don't allow client to read outside of document root.
        if (!resource.toString().contains(docRoot.toString())) {
            sendResponse("403 Forbidden");
            return;
        }

        // If it's a directory, append the default "index.html"
        // filename to it.
        if (Files.isDirectory(resource))
            resource = resource.resolve("index.html");

        // See if the resource is "Not Found".
        if (!Files.exists(resource) || Files.isHidden(resource)) {
            sendResponse("404 Not Found");
            return;
        }
          
        // See if the resource is "Forbidden".
        if (!Files.isReadable(resource) || !Files.isRegularFile(resource)) {
            sendResponse("403 Forbidden");
            return;
        }
          
        // Send OK header and the resource.
        sendResponse("200 OK", Files.readAllBytes(resource));
    }

    /**
     * Process the client request string passed in 'reqStr'.
     *
     * Valid requests: GET, and also exit / null / empty string
     *
     * @param reqStr the request string received from the
     *               client
     */
    public void parseRequest(String reqStr) throws IOException {
        //
        // Tokenize the request string using '?', '&' and ' ' as
        // delimiters:
        //
        //   GET /index.html?country=us&system=linux HTTP/1.1
        //
        // will be tokenized as:
        //
        //   GET
        //   /index.html
        //   country=us
        //   system=linux
        //   HTTP/1.1
        //
        StringTokenizer tokenizer = new StringTokenizer(reqStr, " ?&");
        String token = "";
        if (tokenizer.hasMoreTokens())
            token = tokenizer.nextToken();
          
        // First token is request keyword (e.g. GET, POST, etc.).
        switch (token) {
            case "GET":
                // Second token is resource path (e.g. "/index.html", etc.)
                handleGETRequest(tokenizer.nextToken());
                break;

            case "exit":
            //case "null":
            //case "":
                // Acknowledge and exit.
                sendResponse("200 OK");
                System.out.println("\nExiting at client's request.");
                System.exit(0);
                break;

            case "DELETE":
            case "HEAD":
            case "POST":
            case "PUT":
                sendResponse("501 Not Implemented");
                break;

            default:
                // Unrecognized request keyword.
                sendHeader("400 Bad Request ", 0);
                break;
        }
    }

    /**
     * Reads an HTTP request from the session socket and
     * executes it.
     */
    public void completeSession() throws IOException {
        BufferedReader inReader = new BufferedReader(
                new InputStreamReader(this.m_socket.getInputStream()));
          
        // Read request line from client.
        String reqStr = inReader.readLine();

        // Log the request on the server's console.
        System.out.println(
                m_socket.getRemoteSocketAddress().toString() + " -- " +
                        '\"' + reqStr + '\"');
          
        // Read rest of request header.
        String headerField;
        while ((headerField = inReader.readLine()) != null) {
            if (m_showClientHeader)
                System.out.println(headerField);
            if (headerField.equals(""))
                break;
        }

        // This parses and executes the request.
        if (reqStr != null)
            parseRequest(reqStr);

        // Done; close streams and socket as per the assignment.
        m_outWriter.close();
        m_outStream.close();
        m_socket.close();
    }

    /**
     * Handles one client's HTTP request in its own thread.
     *
     * Overrides Thread.run().
     */
    @Override public void run() {
        try {
            completeSession();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
};



/* EOF */
