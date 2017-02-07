package net.server;

import java.io.Console;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 *
 * @author kevintjuh93
 */
public class CreateINI {

    private static Scanner scanner;

    public static String readLine(String print) {
        System.out.println(print);
        return scanner.nextLine();
    }

    public static void main(String args[]) {
        scanner = new Scanner(System.in);

        StringBuilder sb = new StringBuilder();
        String nextline = "\r\n";//Because I can, and it's free.
        byte worlds;

        System.out.println("Welcome to MoopleDEV's .ini creator\r\n\r\n");

        sb.append("#MoopleDEV's INI file. Do NOT modify it if you are an idiot (:\r\n");
        sb.append("#Flag types: 0 = nothing, 1 = event, 2 = new, 3 = hot\r\n\r\n");

        System.out.println("Flag types: 0 = nothing, 1 = event, 2 = new, 3 = hot\r\n\r\n");

        worlds = Byte.parseByte(readLine("Number of worlds: "));
        sb.append("worlds=").append(worlds).append("\r\n\r\n");

        System.out.println("\r\n");

        for (byte b = 0; b < worlds; b++) {
            sb.append("#Properties for world ").append(b).append("\r\n");

            System.out.println("Properties for world " + b);
            if (b > 1) {
                System.out.println("Make sure you create a npc folder for this world!");
            }

            sb.append("flag").append(b).append("=").append(
                    Integer.parseInt(readLine("   Flag: "))).append("\r\n");

            sb.append("servermessage").append(b).append("=").append(
                    readLine("   Server message: ")).append("\r\n");

            sb.append("eventmessage").append(b).append("=").append(
                    readLine("   Event message: ")).append("\r\n");

            sb.append("whyamirecommended").append(b).append("=").append(
                    readLine("   Recommend message: ")).append("\r\n");

            sb.append("channels").append(b).append("=").append(
                    Byte.parseByte(readLine("   Number of channels: "))).append("\r\n");

            sb.append("exprate").append(b).append("=").append(
                    Integer.parseInt(readLine("   Exp rate: "))).append("\r\n");

            sb.append("droprate").append(b).append("=").append(
                    Integer.parseInt(readLine("   Drop rate: "))).append("\r\n");

            sb.append("mesorate").append(b).append("=").append(
                    Integer.parseInt(readLine("   Meso rate: "))).append("\r\n");

            sb.append("bossdroprate").append(b).append("=").append(
                    Integer.parseInt(readLine("   Boss drop rate: "))).append("\r\n");

            System.out.println(nextline);
            sb.append("\r\n");
        }

        sb.append("\r\n").append("gmserver=").append(Boolean.parseBoolean(readLine("Do you want a GM Server? (true/false)")));
        FileOutputStream out = null;
        try {
            out = new FileOutputStream("moople.ini", false);
            out.write(sb.toString().getBytes());
        } catch (Exception ex) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
            }
        }

        sb = new StringBuilder();
        try {
            System.out.println("\r\nYou are about to set the Java Heap Size, if you don't know what it is, type '?'.");
            String heapsize = readLine("Java Heap Size (in MB): ");
            while (heapsize.equals("?")) {
                System.out.println("\r\n");
                System.out.println("WikiAnswers: Java heap is the heap size allocated to JVM applications which takes care of the new objects being created. If the objects being created exceed the heap size, it will throw an error saying memoryOutof Bound\r\n");
                System.out.println("I recommend using 64 bit with the heap size around 4000, if you have 4 gb RAM.");
                heapsize = readLine("Java Heap Size (in MB): ");
            }
            String linux = readLine("\r\nAre you using a Linux platform or not? (y/n):");
            while (!linux.equals("y") && !linux.equals("n")) {
                System.out.println("Type 'y' if you use linux else type 'n'.");
                linux = readLine("Are you using a Linux platform or not? (y/n):");
            }
            if (linux.equals("n")) {
                out = new FileOutputStream("launch_server.bat", false);
                sb.append("@echo off").append("\r\n").append("@title MoopleDEV Server v83").append("\r\n");
                sb.append("set CLASSPATH=.;dist\\*\r\n");
                sb.append("java -Xmx").append(heapsize).append("m -Dwzpath=wz\\ net.server.Server\r\n");
                sb.append("pause");
            } else {//test
                out = new FileOutputStream("launch_server.sh", false);
                sb.append("#!/bin/sh").append("\r\n\r\n");
                sb.append("export CLASSPATH=\".:dist/*\" \r\n\r\n");
                sb.append("java -Dwzpath=wz/ \\\r\n");
                sb.append("-Xmx").append(heapsize).append("m ").append("net.server.Server");
                System.out.println("Use DOS2UNIX command to convert the .sh file once again.");
            }
            out.write(sb.toString().getBytes());
        } catch (Exception ex) {
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
            }
        }

        System.out.println("\r\nMake sure that ServerConstants in modified too, and clean+compiled before you start the server.");
        System.out.println("If you want other settings; restart this .bat or modify the moople.ini");
    }
}
