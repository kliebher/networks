import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class webserver implements Runnable{

    //vorab initializierungen:
    static final File Root = new File("."); //root pfad
    static final String main_file = "index.html"; // seite die beim Verbinden geladen wird
    static final String not_existing_file = "not_found.html"; //404 Fehler, falls die Seite nicht gefunden wird
    static final String not_supported_method = "wrong_method.html"; //seite, falls nicht unterstütze methode verwedet wurde
    static final String wrong_symbol = "symbol_error.html"; 
    static final int port = 8000;

    private Socket client;

    public webserver(Socket s){  //constructor für den server
        client = s;
    }

    public static void main(String[] args){
        try {
            ServerSocket ss = new ServerSocket(port);
            System.out.println("Server auf Port "+port+" gestartet.."); // wartet auf client verbindung

            while(true){
                webserver webserver = new webserver(ss.accept());

                    System.out.println("Verbindung akzeptiert, "+new Date());

                    Thread t = new Thread(webserver);  // threads für Requestbearbeitung
                    t.start();
            }

        } catch (IOException e) {
            System.err.println("Verbindungsfehler.."+e.getMessage());
        }
    }

    @Override
    public void run() {
        //vorab initialisierung außerhalb des try-catch-blocks um es auch außerhalb problemlos nutzen zu können
        BufferedReader inputs = null;
        PrintWriter outputs = null;
        BufferedOutputStream datas = null;
        String angefragteDatei = null;
        StringTokenizer st = null;
        String genutzteMethode = null;

        try {
            // buffered reader zum lesen des inputs
            inputs = new BufferedReader(new InputStreamReader(client.getInputStream()));
            // print writer & buffered out stream für die Antworten vom server
            outputs = new PrintWriter(client.getOutputStream());
            datas = new BufferedOutputStream(client.getOutputStream());

            //bearbeiten des inputs
            String input = inputs.readLine();
            try {
                st = new StringTokenizer(input);

            }catch(NullPointerException np){
                System.err.println("");
            }
            if (st != null) {
                genutzteMethode = st.nextToken().toUpperCase();
                angefragteDatei = st.nextToken().toLowerCase();
            }


            if (genutzteMethode != null) {
                if (!genutzteMethode.equals("GET")) { // wenn nicht GET
                    System.out.println("Fehler 501: Nicht unterstütze Methode: " + genutzteMethode);

                    //die html seite, welche anzeigt wird bei nicht unterstützer methode
                    File methodenFehler = new File(Root, not_supported_method);
                    int dateiGroesse = (int) methodenFehler.length();
                    String dateiTyp = "text/html";
                    byte[] data = leseDaten(methodenFehler, dateiGroesse);

                    //Outputs über PrintWriter
                    outputs.println("HTTP/1.1 501 Not Implemented");
                    outputs.println("Server: Netzwerke Webserver von Kjell");
                    outputs.println("Date: " + new Date());
                    outputs.println("Content-type: " + dateiTyp);
                    outputs.println("Content-length: " + dateiGroesse);
                    outputs.println();
                    outputs.flush();

                    datas.write(data, 0, dateiGroesse);
                    datas.flush();

                }if(genutzteMethode.equals("GET") && angefragteDatei.endsWith("time=24")){ // "Uhrzeit 24"
                    outputs.printf("Aktuell ist es: %tT\n", new Date()); //%tT entspricht 24h Format
                    outputs.flush();
                    System.out.println("Uhrezit im Format <24h> ausgegeben");

                }else if(genutzteMethode.equals("GET") && angefragteDatei.endsWith("time=12")){ // "Uhrzeit 12"
                    outputs.printf("Aktuell ist es: %tr\n", new Date()); //%tT entspricht 12h Format
                    outputs.flush();
                    System.out.println("Uhrezit im Format <12h> ausgegeben");

                } else if(genutzteMethode.equals("GET") && angefragteDatei.contains("rechne")){ // rechnen
                    ArrayList <Integer> operants = new ArrayList<>();
                    String operator="";

                    String s = angefragteDatei;
                    Pattern p = Pattern.compile("[0-9]+"); //danach sucht er im String
                    Matcher m = p.matcher(s);
                    while(m.find()){
                        int i = Integer.parseInt(m.group()); // String -> int
                        operants.add(i);
                    } // matcher um mehrstellige Zahlen zu erkennen (10, 100, 1000, ..)

                    for(int i = 0; i < angefragteDatei.length(); i++){
                        char c = angefragteDatei.charAt(i);
                        switch(c){
                            case '+':
                                operator = "+";
                                break;
                            case '-':
                                operator = "-";
                                break;
                            case '*':
                                operator = "*";
                                break;
                            case '/':
                                operator = "/";
                                break;
                            case '^':
                                operator = "^";
                                break;
                            case '%':
                                operator = "%";
                                break;
                        }
                    }
                    double ergebnis = 0;
                    if(operator.equals("+")){
                        ergebnis = operants.get(0)+operants.get(1);
                    }
                    if(operator.equals("-")){
                        ergebnis = operants.get(0)-operants.get(1);
                    }
                    if(operator.equals("*")){
                        ergebnis = operants.get(0)*operants.get(1);
                    }
                    if(operator.equals("/")){
                        ergebnis = (double)operants.get(0)/(double)operants.get(1);
                    }
                    if(operator.equals("^")){
                        ergebnis = Math.pow(operants.get(0), operants.get(1));
                    }
                    if(operator.equals("%")){
                        ergebnis = (double)operants.get(0)%(double)operants.get(1);
                    }
                    String test = ""+ergebnis;
                    if(test.endsWith(".0")){ // um .0 wegzulassen, falls vorhanden
                        int ergebnis2 = (int)(ergebnis);
                        outputs.print("Ergebnis: ");
                        outputs.print(operants.get(0)+operator+operants.get(1)+" = "+ergebnis2);
                    }else{
                        outputs.print("Ergebnis: ");
                        outputs.print(operants.get(0)+operator+operants.get(1)+" = "+ergebnis);
                    }
                    outputs.flush();
                    System.out.println("Ergebnis der Rechnung <"+operants.get(0)+operator+operants.get(1)+"> ausgegeben");

                } else{ // normal
                    if (angefragteDatei.endsWith("/")) {
                        angefragteDatei += main_file;
                    }
                    File datei = new File(Root, angefragteDatei);
                    int dateiGroesse = (int) datei.length();
                    String dateiTyp = dateitypAusgeben(angefragteDatei);


                    if (genutzteMethode.equals("GET")) {
                        byte[] data = leseDaten(datei, dateiGroesse);

                        //Outputs über PrintWriter
                        outputs.println("HTTP/1.1 200 OK");
                        outputs.println("Server: Netzwerke Webserver von Kjell");
                        outputs.println("Date: " + new Date());
                        outputs.println("Content-type: " + dateiTyp);
                        outputs.println("Content-length: " + dateiGroesse);
                        outputs.println();
                        outputs.flush();

                        datas.write(data, 0, dateiGroesse);
                        datas.flush();
                    }
                    System.out.println("Angefragte <" + dateiTyp + "> Datei <" + angefragteDatei + "> ausgegeben");
                }
            }

        }catch(FileNotFoundException f){
                try{
                    dateiExistiertNicht(outputs, datas, angefragteDatei);
                }catch(IOException io){
                    System.err.println("Datei nicht gefunden : "+io.getMessage());
                }

        } catch (IOException io) {
            System.err.println("Server Fehler "+io.getMessage());
        }finally {
            try{
                // zum schluss alle laufenden dinge beenden, ein finally block eignet sich dafür gut
                inputs.close();;
                outputs.close();
                datas.close();
                client.close();
            }catch(Exception e){
                System.err.println("Fehler beim schließen der Streams "+e.getMessage());
            }
                System.out.println("Server wartet auf weitere Anfragen..\n");
        }
    }

    private byte[] leseDaten(File datei, int dateiGroesse) throws IOException {
        FileInputStream input = null; // vorab initialisierung, damit ich im finally block darauf zugreifen kann
        byte[] data = new byte[dateiGroesse];
        try {
            input = new FileInputStream(datei);
            input.read(data);
        }finally {
            if (input != null){
                input.close();
            }
        }
        return data;
    }

    private String dateitypAusgeben(String angefragteDatei){
        if(angefragteDatei.endsWith(".html")){
            return "text/html";
        }
        else if(angefragteDatei.endsWith(".htm")){
            return "text/html";
        }
        else if(angefragteDatei.endsWith(".gif")){
            return "image/gif";
        }
        else if(angefragteDatei.endsWith(".png")){
            return "image/png";
        }
        else if(angefragteDatei.endsWith(".jpeg")){
            return "image/jpeg";
        }
        else if(angefragteDatei.endsWith(".jpg")){
            return "image/jpg";
        }
        else return "text/plain";
    }

    private void dateiExistiertNicht(PrintWriter outputs, OutputStream datas, String angefragteDatei) throws IOException {
        File dateiExistiertNicht;
        if(angefragteDatei.contains("..")){
            dateiExistiertNicht = new File(Root, wrong_symbol);
        }else{
            dateiExistiertNicht = new File(Root, not_existing_file);
        }

        int dateiGroesse = (int)dateiExistiertNicht.length();
        String dateiTyp = "text/html";
        byte[] data = leseDaten(dateiExistiertNicht, dateiGroesse);

        //Outputs über PrintWriter
        outputs.println("HTTP/1.1 404 File Not Found");
        outputs.println("Server: Netzwerke Webserver von Kjell");
        outputs.println("Date: "+ new Date());
        outputs.println("Content-type: "+dateiTyp);
        outputs.println("Content-length: "+dateiGroesse);
        outputs.println();
        outputs.flush();
        datas.write(data, 0, dateiGroesse);
        datas.flush();
        System.out.println("Datei "+angefragteDatei+" nicht gefunden");
    }

}
