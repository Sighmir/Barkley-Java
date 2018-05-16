package br.mackenzie.cd.udp;

import br.mackenzie.cd.models.NTPObject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class UDPClient {

    public static byte[] sendData = new byte[1024];
    public static byte[] receiveData = new byte[1024];
    public static DatagramSocket clientSocket;
    public static InetAddress IPAddress;
    public static String logfile = "";
    public static Long time;
    public static int port;
    public static String address;
    // Por algum motivo o client nao tem nenhum argumento apontando o servidor.
    public static NTPObject server;
    
    public static void start(String args[]) {
        if (args.length == 4) {
            // Configure o IP do server aqui. Nao ha especificacao.
            String hostname ="localhost";
            int hostport = 8000;
            try {
                server = new NTPObject(InetAddress.getByName(hostname),hostport); 
            } catch (UnknownHostException ex) {
                log("Host desconhecido: "+hostname);
            }
            String [] arg = args[1].split(":");
            try {
                IPAddress = InetAddress.getByName(arg[0]);
            } catch (UnknownHostException ex) {
                log("Host desconhecido: "+arg[0]);
            }
            port = Integer.parseInt(arg[1]);
            time = Long.parseLong(args[2]);
            logfile = args[3];
            try {
                clientSocket = new DatagramSocket(port,IPAddress);
            } catch (SocketException ex) {
                log("Falha ao criar socket.");
            }
            address = (IPAddress.getHostAddress()+":"+port);
            receivePacket();
            receiveInput();
            timer();
            log("NTP Client started on " + IPAddress + ":" + port);
        } else {
            log("Usage: java -jar RelogioUDP.jar -s <ip:port> <time> <logfile>");
        }
    }
    
    public static void receivePacket() {
        new Thread(new Runnable() {
            public void run(){
                while(true) {
                    receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    try {
                        clientSocket.receive(receivePacket);
                    } catch (IOException ex) {
                        log("Falha ao receber pacote.");
                    }
                    String packet = new String(receivePacket.getData()).trim();
                    log("CLIENT <- " + packet);
                    String [] pack = packet.split(" ");
                    if (pack[1].equals("getTime")) {
                        sendTime();
                    } else if (pack[1].equals("setTime")) {
                        setTime(Long.parseLong(pack[2]));
                    }
                }
            }
        }).start();
    }    
    
    public static void receiveInput() {
        // PDF 1
        new Thread(new Runnable() {
            public void run(){
                while(true) {
                    try {
                        Scanner scanner = new Scanner(System.in);
                        String input = scanner.nextLine();
                        String [] arg = input.split(":");
                        if (input.equals("getHora")) {
                            getHora();
                        } else if (arg[0].equals("corrigeHora")) {
                            if (arg.length == 2)
                                corrigeHora(Long.parseLong(arg[1]));
                            else
                                corrigeHora();
                        }
                    } catch (Exception ex) {
                        log("Erro de entrada ou programa finalizando.");
                    }
                }
            }
        }).start();
    }    
    
    public static void setTime(Long t) {
        Long my_time = time;
        Long sv_time = t;
        Long diff = Math.abs(my_time-sv_time);
        String d = diff.toString();
        if (my_time > sv_time) {
            d = "-"+d;
            time -= diff;
        } else {
            d = "+"+d;
            time += diff;
        }
        log("CLIENT: Meu relogio era " + my_time + " o relogio do server é " + sv_time + " mudança realizada de " + d + " segundos");
    }
    
    public static void sendTime() {
        sendData = new byte[1024];
        String data = address+" time "+time.toString();
        log("CLIENT -> " + data);
        sendData = data.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, server.IPAddress, server.port);
        try {     
            clientSocket.send(sendPacket);
        } catch (IOException ex) {
            log("Falha ao enviar pacote: "+data);
        }
    }
       
    public static void getHora() {
        // PDF 1
        sendData = new byte[1024];
        String data = address+" getHora";
        log("CLIENT -> " + data);
        sendData = data.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, server.IPAddress, server.port);
        try {     
            clientSocket.send(sendPacket);
        } catch (IOException ex) {
            log("Falha ao enviar pacote: "+data);
        }
    }    
    
    public static void corrigeHora(Long t) {
        // PDF 1
        sendData = new byte[1024];
        String data = address+" corrigeHora "+t.toString();
        log("CLIENT -> " + data);
        sendData = data.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, server.IPAddress, server.port);
        try {
            clientSocket.send(sendPacket);
        } catch (IOException ex) {
            log("Falha ao enviar pacote: "+data);
        }
    }
    
    public static void corrigeHora() {
        // PDF 1
        sendData = new byte[1024];
        Long millis = System.currentTimeMillis();
        String data = address+" corrigeHora "+millis.toString();
        log("CLIENT -> " + data);
        sendData = data.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, server.IPAddress, server.port);   
        try {
            clientSocket.send(sendPacket);
        } catch (IOException ex) {
            log("Falha ao enviar pacote: "+data);
        }
    }
    
    public static void timer() {
        new Thread(new Runnable() {
            public void run(){
                while(true) {
                    try {
                        time++;
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        log("Timer interrompido.");
                    }
                }
            }
        }).start();       
    }
    
    public static void log(String text) {
        try {
            String timestamp = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy - ").format(new Date());
            System.out.println(timestamp+text);
            
            if (logfile.isEmpty())
                return;
            Writer output;
            output = new BufferedWriter(new FileWriter(logfile, true));
            output.append(timestamp+text+"\n");
            output.close();
        } catch (IOException ex) {
            System.out.println("Erro escrevendo log no arquivo.");
        }
    }
}
