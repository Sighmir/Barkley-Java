/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.mackenzie.cd.udp;

import br.mackenzie.cd.models.NTPObject;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author 31416489
 */
public class UDPServer {
    public static Map<String, NTPObject> clients = new HashMap<String, NTPObject>();
    public static byte[] receiveData = new byte[1024];
    public static byte[] sendData = new byte[1024];
    public static DatagramSocket serverSocket;
    public static InetAddress IPAddress;
    public static String slavesfile;
    public static String logfile = "";
    public static Long time;
    public static int port;
    public static int d;
    public static String address;
    
    public static void start(String args[]) {
        if (args.length == 6) {
            String [] arg = args[1].split(":");
            try {
                IPAddress = InetAddress.getByName(arg[0]);
            } catch (UnknownHostException ex) {
                log("Host desconhecido: "+arg[0]);
            }
            port = Integer.parseInt(arg[1]);
            time = Long.parseLong(args[2]);
            d = Integer.parseInt(args[3]);
            slavesfile = args[4];
            logfile = args[5];
            try {
                serverSocket = new DatagramSocket(port,IPAddress);
            } catch (SocketException ex) {
                log("Falha ao criar socket.");
            }
            address = (IPAddress.getHostAddress()+":"+port);
            processClients();
            receivePacket();
            timePoller();
            timer();
            log("NTP Server started on " + IPAddress + ":" + port);
        } else {
            log("Usage: java -jar RelogioUDP.jar -m <ip:port> <time> <d> <slavesfile> <logfile>");
        }
    }
    
    public static void receivePacket() {
        new Thread(new Runnable() {
            public void run(){
                while(true) {
                    receiveData = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    try {
                        serverSocket.receive(receivePacket);
                    } catch (IOException ex) {
                        Logger.getLogger(UDPServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    String packet = new String(receivePacket.getData()).trim();
                    log("SERVER <- " + packet);
                    String [] pack = packet.split(" ");
                    NTPObject client = clients.get(pack[0]);
                    if (pack[1].equals("time")) {
                        client.time = Long.parseLong(pack[2]);
                    } else if (pack[1].equals("getHora")) {
                        sendHora(client);
                    } else if (pack[1].equals("corrigeHora")) {
                        corrigeHora(client,Long.parseLong(pack[2]));
                    }
                }
            }
        }).start();
    }
    
    public static void sendTime(NTPObject client) {
        getTimeAverage();
        sendData = new byte[1024];
        String data = address+" setTime "+time.toString();
        log("SERVER -> " + data);
        sendData = data.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, client.IPAddress, client.port);
        try {
            serverSocket.send(sendPacket);
        } catch (IOException ex) {
            log("Falha ao enviar pacote: "+data);
        }
    }
    
    public static void broadcastTime() {
        getTimeAverage();
        clients.forEach((id, client) -> {
            sendData = new byte[1024];
            String data = address+" setTime "+time.toString();
            log("SERVER -> " + data);
            sendData = data.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, client.IPAddress, client.port);
            try {
                serverSocket.send(sendPacket);
            } catch (IOException ex) {
                log("Falha ao enviar pacote: "+data);
            }
        });     
    }
    
    public static void timePoller() {
        new Thread(new Runnable() {
            public void run(){
                while(true) {
                    try {                         
                        clients.forEach((id, client) -> {
                            sendData = new byte[1024];
                            String data = address+" getTime";
                            log("SERVER -> " + data);
                            sendData = data.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, client.IPAddress, client.port);
                            try {
                                serverSocket.send(sendPacket);
                            } catch (IOException ex) {
                                log("Falha ao enviar pacote: "+data);
                            }
                        });
                        Thread.sleep(1000);
                        broadcastTime();
                        Thread.sleep(29000);
                    } catch (InterruptedException ex) {
                        log("Poller interrompido.");
                    }
                }
            }
        }).start();       
    }
    
    public static void sendHora(NTPObject client) {
        // PDF 1
        sendData = new byte[1024];
        Long millis = System.currentTimeMillis();
        String data = address+" "+millis.toString();
        log("SERVER -> " + data);
        sendData = data.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, client.IPAddress, client.port);
        try {   
            serverSocket.send(sendPacket);
        } catch (IOException ex) {
            log("Falha ao enviar pacote: "+data);
        }
    }
    
    public static void corrigeHora(NTPObject client, Long t) {
        // PDF 1
        Long my_time = System.currentTimeMillis();
        Long cli_time = t;
        Long diff = Math.abs(my_time-cli_time);
        String d = diff.toString();
        if (my_time > cli_time) {
            d = "-"+d;
        } else {
            d = "+"+d;
        }
        log("SERVER: Meu relogio é " + my_time + " o relogio de " + client.IPAddress + ":" + client.port + " é " + cli_time + " mudança necessaria de " + d + " milisegundos");
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
    
    public static void processClients() {
        try (Stream<String> stream = Files.lines(Paths.get(slavesfile))) {
            stream.forEach((line) -> {
                String [] arg = line.split(":");
                try {
                    NTPObject client = new NTPObject(InetAddress.getByName(arg[0]), Integer.parseInt(arg[1]));
                    clients.put(client.IPAddress.getHostAddress()+":"+client.port, client);
                } catch (UnknownHostException ex) {
                    log("Host desconhecido: "+arg[0]);
                }
            });
        } catch (IOException ex) {
            log("Nao foi possivel encontrar slavesfile.");
        }
    }
    
    public static void getTimeAverage() {
        int[] i = {0}; 
        clients.forEach((address, client) -> {
            if (client.time != null && Math.abs(client.time-time) < d) {
                time += client.time;
                i[0]++;
            }
        });
        time = time/(i[0]+1);
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
