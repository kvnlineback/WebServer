
/*--------------------------------------------------------

1. Name / Date: Kevin Lineback - 02/01/2020


2. Java version 1.8.0_211-b12

3. Precise command-line compilation examples / instructions:

javac MyWebServer.java


4. Precise examples / instructions to run this program:

java MyWebServer
enter http://localhost:2540/ or http://localhost:2540 into the browser after connecting to get the root directory
enter http://localhost:2540/ followed by a file name or directory to open that specific item within the root directory, subfiles will display also

5. List of files needed for running the program.

MyWebServer.java

5. Notes:

Everything should work fine, please test with Mozilla, chrome is very difficult to handle the favicons. Still works perfect with Mozilla. I did not restrict access to directory tree
because I was only running my server offline.
Please forgive my messy, verbose code, I wanted to clean it up but didn't want to risk messing with functionality

----------------------------------------------------------*/

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;

class Worker extends Thread { // Class definition
	public String topLine = ""; // this will hold the get request
	private String conType = ""; // this holds the connection type;
	Socket socket; // declaring a socket class
	private static String fileToSend; // this will hold either the dir name or file name
	private File file; // this will be the file or dir to open
	static final byte[] EOL = { (byte) '\r', (byte) '\n' }; // new line chars to send for headers
	private boolean atRoot; // this tells me if im in my root so I can append the correct directory path to
							// the hyperlinks

	Worker(Socket sock) {
		socket = sock; // using constructor to pass in a certain socket and assign it to a worker
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		PrintStream output = null; // declaring output
		BufferedReader input = null; // declaring input
		try {
			output = new PrintStream(socket.getOutputStream()); // open socket output stream
			input = new BufferedReader(new InputStreamReader(socket.getInputStream())); // open socket input stream
			if (input != null) { // dont do anything if we dont open a stream
				topLine = input.readLine(); // otherwise get the Get request
				if (topLine != null) { // continue if we actually read it in
					System.out.println("received request: " + topLine);
					if (!topLine.contains("favicon")) { // skipping favicon *NOTE: favicons will still interrupt this
														// server even with the icon in the root when using CHROME,
														// please test in mozilla!*
						if (topLine.contains("fake-cgi")) { // checking to see if addNum sent data
							String[] data = topLine.split(" "); // these lines below are just parsing the addNum input
																// to send to addNum function, kind of rudimentary but
																// that's all I could think of, still works fine
							String data2 = data[1];
							String[] data3 = data2.split("=");
							String name = data3[1].substring(0, data3[1].indexOf("&"));
							String firstNum = data3[2].substring(0, data3[2].indexOf("&"));
							String secondNum = data3[3];
							int temp1 = Integer.parseInt(firstNum);
							int temp2 = Integer.parseInt(secondNum);
							int temp3 = temp1 + temp2;
							String thirdNum = Integer.toString(temp3);
							addNums(output, firstNum, secondNum, thirdNum, name);
							socket.close();
							return;
						} else {
							String[] data = topLine.split(" ");
							if (data.length > 0) {
								fileToSend = data[1].substring(1); // parsing file or directory name from the get
																	// request
							}

							if (fileToSend.endsWith("..") || fileToSend.endsWith("/.ht") || fileToSend.endsWith("~")) { // checking
																														// for
																														// potential
																														// security
																														// breaches
								throw new Exception("cannot access");
							}

							if (fileToSend.contains(".txt") || fileToSend.contains(".java") // this will decide if we
																							// set type to html or text,
																							// it also handles addNum
																							// requests
									|| fileToSend.contains(".class") || fileToSend.contains(".doxc")
									|| fileToSend.endsWith("/")) {
								conType = "text/plain";
							} else {
								conType = "text/html";
							}
							if (fileToSend.endsWith("/") || (!topLine.contains(".txt") && !topLine.contains(".html") // checking
																														// to
																														// see
																														// if
																														// we
																														// should
																														// process
																														// a
																														// file
																														// or
																														// directory
									&& !topLine.contains(".java") && !topLine.contains(".docx")
									&& !topLine.contains(".class"))) {
								StringBuilder sb = new StringBuilder();
								if (!new File(fileToSend).exists()) {
									file = new File(getRootDir()); // get root dir
									fileToSend = getRootDir();
									atRoot = true; // we are at the root to start
								} else {
									file = new File(fileToSend); // use path name provided
									atRoot = false; // we are not at the root to start
								}
								File[] files = file.listFiles(); // get the list of files at this dir
								sb.append("<pre>\n"); // setting up html to send
								sb.append("\n");
								if (atRoot) {
									sb.append("<h1>" + "Index of " + getRootDir() + "<h1>\n"); // naming the index in
																								// html
								} else {
									sb.append("<h1>" + "Index of " + fileToSend + "<h1>\n");
								}
								sb.append(String.format("<a href=\"%s\">%s</a> <br>\n",
										"http://localhost:2540" + "/" + file.getParent() + "/", "Parent Directory")); // setting
																														// back
																														// button
								sb.append("\n");
								for (File f : files) { // iterating through the dir
									if (f.isDirectory()) { // check to see if we're at a file or another dir
										String newName = f.getName() + "/";
										sb.append(String.format("<a href=\"%s\">%s</a> <br>\n",
												"http://localhost:2540" + "/" + f.getParent() + "/" + newName,
												newName)); // add the hyperlink to html string

									} else if (f.isFile()) {
										if (atRoot) { // this will determine whether to add the parent name
											sb.append(String.format("<a href=\"%s\">%s</a> <br>\n",
													"http://localhost:2540" + "/" + f.getName(), f.getName())); // add
																												// the
																												// file
																												// hyperlink
																												// to
																												// html
																												// string
										} else {
											sb.append(String.format("<a href=\"%s\">%s</a> <br>\n",
													"http://localhost:2540" + "/" + f.getParent() + "/" + f.getName(),
													f.getName()));
										}
									}
								}
								System.out
										.println("Kevin's webserver is sending the following directory: " + fileToSend); // tell
																															// console
																															// what
																															// we
																															// sent

								output.println("HTTP/1.1 200 OK"); // header line
								output.println("Content-Length: " + sb.toString().length()); // header line
								output.println("Content-type: text/html"); // header line
								output.print(EOL); // header new line char
								output.println(sb.toString()); // sending actual html
								output.flush(); // clear the stream

							} else { // processing a file here
								fileToSend = URLDecoder.decode(fileToSend); // remove spaces from file
								file = new File(fileToSend); // open file
								if (!file.exists()) {
									System.out.println("the file " + fileToSend + " does not exist"); // check to see if
																										// the file
																										// exists
									throw new Exception();
								}
								System.out.println("Kevin's webserver is sending the following file: " + fileToSend); // tell
																														// console
																														// what
																														// we
																														// are
																														// sending
								output.println("HTTP/1.1 200 OK"); // html header lines below
								output.println("Content-Length: " + file.length());
								output.println("Content-type: " + conType);
								output.write(EOL);
								output.println(readFile()); // sending the actual file data after header
								output.flush();

							}
							socket.close();
						}
					} else {
						System.out.println("Ignoring favicon request"); // got a favicon request, ignoring it
					}
				}
			}

		} catch (IOException e) { // error handling on these lines, helped a lot with debugging
			System.out.println(e);
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}
	}

	public static void addNums(PrintStream out, String first, String second, String third, String name) { // addnums
																											// function,
																											// this just
																											// processes
																											// the
																											// parsed
																											// string
																											// sent back
																											// and sends
																											// a header
																											// followed
																											// by
																											// formatted
																											// html
		String data = ("Dear " + name + ", the sum of " + first + " and " + second + " is " + third);
		System.out.println("Sending " + data);
		out.println("HTTP/1.1 200 OK");
		out.println("Content-Length: " + data.length());
		out.println("Content-type: text/html");
		out.print(EOL);
		out.println("<html>\n");
		out.println(data);
		out.println("</html>");
		out.flush();

	}

	public static String getRootDir() { // used provided getRootDir function when I needed to get the root
		File file = new File(".");
		String directoryRoot = "";
		try {
			directoryRoot = file.getCanonicalPath();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return directoryRoot;
	}

	public static String readFile() { // this is a helper function to read the file into a string to send back to
										// client, pretty basic file reader
		String result = "";
		try {

			BufferedReader reader = new BufferedReader(new FileReader(fileToSend)); // open file
			String receiveString = ""; // string to test if its at the end
			StringBuilder stringBuilder = new StringBuilder();
			while ((receiveString = reader.readLine()) != null) {
				stringBuilder.append(receiveString);
			}
			reader.close();
			result = stringBuilder.toString(); // putting the filedata in to a string
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result; // giving string back to process and send
	}
}

public class MyWebServer {

	public static void main(String a[]) throws IOException { // pretty basic server handler from jokeserver

		int q_len = 6; // max queue
		int port = 2540; // current port
		Socket sock; // current socket
		ServerSocket servsock = new ServerSocket(port, q_len);
		System.out.println("Kevin's Web Server starting up at port 2540.\n");
		while (true) {
			// wait for the next client connection:
			sock = servsock.accept();
			new Worker(sock).start();
		}
	}
}
