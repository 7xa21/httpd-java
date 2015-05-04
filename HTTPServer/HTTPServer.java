/*
 * HTTPServer.java - a simple HTTP request server.
 * 
 * Randy Sorensen <sorensra@msudenver.edu>
 * Homework 3 (CS3700 - Computer Networks)
 * 2013-Feb-12
 * 
 * 
 * Usage:
 *   java HTTPServer [-h] [-p PORT] [DOCPATH]
 * 
 *   -h        display this usage screen and exit
 *   -p PORT   listen on TCP port PORT (Default: 8080)
 *   -s        display full client request header (Default: no)
 * 
 *   DOCPATH is the document root path from which to serve file
 *   resources.  The default is the current working directory.
 *
 * 
 * Notes:
 *   This program was tested with OpenJDK 7 on Linux Mint 14.
 *   Linux Mint is a popular Ubuntu Linux derivative.
 * 
 *   I tested my server with the provided CS3700.htm file, as well
 *   as other assorted website trees on my computer using Firefox
 *   18 as the client; it works surprisingly well.
 */
import java.io.*;
import java.net.*;
import java.nio.file.*;


/**
 * A very basic and primitive HTTP file server.
 * 
 * The server is initialized with parameters such as the document
 * root path and the port number to listen on.  The server is then
 * "started" with startServer(), which listens for incoming
 * connections forever.  Each connection is handled discretely in
 * its own thread using the HTTPServerSession class.
 * 
 * @author Randy Sorensen
 */
public class HTTPServer {

    // Member variables.
    private boolean m_showClientHeader;
    private String m_docPath;
    private int m_port;


    /**
     * Initializes an HTTP server with default values.
     *
     * Defaults might be overridden in parseArgs().
     */
    public HTTPServer() {
        // Default values.
        this.m_showClientHeader = false;
        this.m_docPath = ".";
        this.m_port = 8080;
    }

    /**
     * Explains the command line argument syntax to the user.
     */
    public void showUsage() {
        System.out.println(
              "Usage:                                                   \n"
            + "\tjava HTTPServer [-h] [-p PORT] [DOCPATH]               \n"
            + "                                                         \n"
            + "  -h        display this usage screen and exit           \n"
            + "  -p PORT   use TCP port number PORT (Default: 8080)     \n"
            + "  -s        display full client request header           \n"
            + "            (Default: no)                                \n"
            + "                                                         \n"
            + " DOCPATH is the document root path from which resources  \n"
            + " will be served.  The default is the current working     \n"
            + " directory.");
    }

    /**
     * Sets the server's member/state variables based on
     * the arguments passed on the command line.
     *
     * @param args the program arguments that were passed to
     *   main().
     */
    public void parseArgs(String args[]) {
        // Process command line arguments.
        for (int arg = 0; arg < args.length; arg++) {
            switch (args[arg]) {
                case "-h":
                    // Display usage and exit.
                    showUsage();
                    System.exit(0);
                    break;    // not reached

                case "-p":
                    // Set TCP port number.
                    arg++;
                    this.m_port = Integer.parseInt(args[arg]);
                    break;

                case "-s":
                    // Show full client request header for each request
                    m_showClientHeader = true;
                    break;

                default:
                    // Set document root path.
                    this.m_docPath = args[arg];
                    if (!Files.isDirectory(Paths.get(m_docPath))) {
                        showUsage();
                        System.exit(-1);
                    }

                    // Kill loop / ignore subsequent arguments.
                    arg = args.length;
                    break;
            }
        }
    }

    /**
     * Listens for HTTP requests "forever".
     *
     * The server can be terminated by sending a blank request
     * line or by sending "null" or "exit" as the request line.
     *
     * @param args The program arguments that were passed to
     *             main()
     */
     public void startServer(String  args[]) throws IOException {
         // Parse arguments, possibly overriding defaults.
         parseArgs(args);

         // Show server info.
         System.out.println("Server:\t\t\t" + InetAddress.getLocalHost());
         System.out.println("TCP Port:\t\t" + m_port);
         System.out.println("Document root:\t\t\"" + m_docPath + '\"');

         //
         // Accept connections forever.
         // Terminate by sending "exit", "null", or simply "" to
         // the server.
         //
         ServerSocket listenSocket = new ServerSocket(this.m_port);
         Socket sessionSocket;
         System.out.println("\nWaiting for connections...");

         for (;;) {
             sessionSocket = listenSocket.accept();
             new HTTPServerSession(
                     m_showClientHeader,
                     sessionSocket,
                     m_docPath
             ).start();
         }
    }

    /**
     * Program entry - starts server and catches exceptions.
     *
     * @param args Array of command line arguments; see
     *             showUsage()
     */

    public static void main(String args[]) {
        System.out.println("HTTP Server (CS3700/Homework 3)");
        System.out.println("   by Randy Sorensen");
        System.out.println("-------------------------------");

        try {
            new HTTPServer().startServer(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
};
