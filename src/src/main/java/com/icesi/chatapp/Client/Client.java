package com.icesi.chatapp.Client;
import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 6789;
    public static volatile boolean llamadaActiva = false;

    private static volatile boolean esperandoEnter = false;
    private static final Object enterLock = new Object();

    public static void main(String[] args) {
        try (
                Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                DataInputStream dataIn = new DataInputStream(socket.getInputStream());
                DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
                Scanner scanner = new Scanner(System.in);
        ) {
            new File("audios_recibidos").mkdirs(); // Asegura que esta carpeta exista

            Thread recibir = new Thread(() -> {
                try {
                    String lineaRecibida; // Cambiado para evitar conflicto de nombre con 'linea' en el main
                    while ((lineaRecibida = in.readLine()) != null) {
                        if (lineaRecibida.equals("AUDIO_INCOMING")) {
                            recibirYReproducirAudio(dataIn);
                        } else if (lineaRecibida.equals("LLAMADA_INCOMING")) {
                            String emisor = dataIn.readUTF();
                            String ip = dataIn.readUTF();
                            int puerto = dataIn.readInt();

                            System.out.println("Llamada entrante de " + emisor + " desde " + ip + ":" + puerto);
                            System.out.print("¿Aceptar llamada? (S/N): ");
                            // Necesitamos un scanner local para este hilo o pasar el principal de forma segura
                            // Para simplificar, creamos uno local para la respuesta
                            Scanner respuestaScanner = new Scanner(System.in);
                            String respuesta = respuestaScanner.nextLine();
                            if (respuesta.equalsIgnoreCase("S")) {
                                new Thread(() -> AudioCallReceiver.iniciarRecepcion(puerto)).start();
                                llamadaActiva = true;
                                out.println("CALL_ACCEPTED");
                            } else {
                                System.out.println("Llamada rechazada.");
                                out.println("CALL_REJECTED");
                            }
                            // No cerramos respuestaScanner aquí, ya que el System.in se cerraría
                        } else if (lineaRecibida.startsWith("IP_DESTINO:")) {
                            String ip = lineaRecibida.split(":")[1];
                            String puertoLine = in.readLine();
                            int puerto = Integer.parseInt(puertoLine.split(":")[1]);

                            System.out.println("Iniciando llamada con " + ip + ":" + puerto + "...");
                            new Thread(() -> AudioCallSender.iniciarLlamada(ip, puerto)).start();
                            new Thread(() -> AudioCallReceiver.iniciarRecepcion(puerto)).start();
                            llamadaActiva = true;
                        } else {
                            System.out.println(lineaRecibida);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Conexión con el servidor cerrada o error de lectura.");
                    // e.printStackTrace();
                }
            });
            recibir.start();

            // Bucle principal para enviar mensajes y comandos
            while (true) {
                String linea; // <--- Declaramos 'linea' aquí dentro del bucle

                synchronized (enterLock) {
                    if (esperandoEnter) {
                        continue;
                    }
                    System.out.print("> ");
                    linea = scanner.nextLine(); // <--- Ahora 'linea' está en el alcance
                }

                if (linea.equals("5")) {
                    out.println("MENU_REQUEST_5");
                    Thread.sleep(100);
                    System.out.println("Ingresa el nombre del usuario destinatario:");
                    String usuario = scanner.nextLine();
                    out.println(usuario);
                    grabarYEnviarAudio(dataOut);
                    continue;
                }

                if (linea.equals("6")) {
                    out.println("MENU_REQUEST_6");
                    Thread.sleep(100);
                    System.out.println("Ingresa el nombre del grupo:");
                    String grupo = scanner.nextLine();
                    out.println(grupo);
                    grabarYEnviarAudio(dataOut);
                    continue;
                }

                if (linea.equals("9")) {
                    out.println("CALL_REQUEST");
                    Thread.sleep(100);
                    System.out.print("Nombre del usuario o grupo a llamar: ");
                    String destinatario = scanner.nextLine();
                    out.println(destinatario);
                    System.out.println("Esperando respuesta del servidor para la llamada...");
                    continue;
                }

                out.println(linea);

                if (linea.equals("4")) {
                    recibir.interrupt();
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("Error en el cliente: " + e.getMessage());
            // e.printStackTrace();
        } finally {
            System.out.println("Cliente desconectado.");
        }
    }

    private static void recibirYReproducirAudio(DataInputStream dataIn) {
        // ... (Este método se mantiene igual, no necesita cambios) ...
        try {
            String emisor = dataIn.readUTF();
            String nombreArchivo = dataIn.readUTF();
            long tamanoArchivo = dataIn.readLong();

            System.out.println("\n Has recibido una nota de voz de " + emisor + " (" + tamanoArchivo + " bytes)");

            File carpeta = new File("audios_recibidos");
            String nombreUnico = System.currentTimeMillis() + "_de_" + emisor + "_" + nombreArchivo;
            File archivoAudio = new File(carpeta, nombreUnico);

            try (FileOutputStream fos = new FileOutputStream(archivoAudio)) {
                byte[] buffer = new byte[4096];
                long bytesRecibidos = 0;

                while (bytesRecibidos < tamanoArchivo) {
                    int bytesParaLeer = (int) Math.min(buffer.length, tamanoArchivo - bytesRecibidos);
                    int bytesLeidos = dataIn.read(buffer, 0, bytesParaLeer);

                    if (bytesLeidos == -1) {
                        throw new IOException("Conexión cerrada inesperadamente");
                    }

                    fos.write(buffer, 0, bytesLeidos);
                    bytesRecibidos += bytesLeidos;
                }
                fos.flush();
            }

            System.out.println("Presiona ENTER para reproducir...");
            esperandoEnter = true;
            System.out.println("Reproduciendo audio...");
            if (reproducirAudio(archivoAudio)) {
                System.out.println("Reproducción completada.");
            } else {
                System.out.println(" Error en la reproducción.");
            }

        } catch (Exception e) {
            System.err.println("Error al recibir audio: " + e.getMessage());
        } finally {
            synchronized (enterLock) {
                esperandoEnter = false;
            }
        }
    }

    private static void grabarYEnviarAudio(DataOutputStream dataOut) {
        // ... (Este método se mantiene igual, no necesita cambios) ...
        try {
            Scanner scannerGrabacion = new Scanner(System.in); // Usar un scanner local para la grabación

            AudioFormat formato = new AudioFormat(44100.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, formato);

            if (!AudioSystem.isLineSupported(info)) {
                System.out.println(" Micrófono no soportado");
                return;
            }

            TargetDataLine microfono = (TargetDataLine) AudioSystem.getLine(info);
            microfono.open(formato);
            microfono.start();

            System.out.println("Grabando... Presiona ENTER para detener");
            ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];

            final boolean[] grabando = {true};

            Thread controlGrabacion = new Thread(() -> {
                try {
                    scannerGrabacion.nextLine(); // Espera el ENTER aquí
                    grabando[0] = false;
                    microfono.stop();
                    microfono.close();
                } catch (Exception e) {
                    System.err.println("Error en control de grabación: " + e.getMessage());
                }
            });
            controlGrabacion.start();

            while (grabando[0] && microfono.isOpen()) {
                int bytesLeidos = microfono.read(buffer, 0, buffer.length);
                if (bytesLeidos > 0) {
                    audioBuffer.write(buffer, 0, bytesLeidos);
                }
            }

            controlGrabacion.join();
            System.out.println("Grabación detenida");

            byte[] audioData = audioBuffer.toByteArray();
            if (audioData.length == 0) {
                System.out.println("No se grabó audio");
                return;
            }

            File archivoTemporal = new File("temp_audio_" + System.currentTimeMillis() + ".wav");

            try (ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
                 AudioInputStream ais = new AudioInputStream(bais, formato, audioData.length / formato.getFrameSize())) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, archivoTemporal);
            }

            dataOut.writeUTF(archivoTemporal.getName());
            dataOut.writeLong(archivoTemporal.length());

            try (FileInputStream fis = new FileInputStream(archivoTemporal)) {
                byte[] bufferEnvio = new byte[4096];
                int bytesLeidos;
                while ((bytesLeidos = fis.read(bufferEnvio)) > 0) {
                    dataOut.write(bufferEnvio, 0, bytesLeidos);
                }
                dataOut.flush();
            }

            if (!archivoTemporal.delete()) {
                System.err.println("Advertencia: No se pudo eliminar el archivo temporal: " + archivoTemporal.getName());
            }

        } catch (Exception e) {
            System.err.println(" Error al grabar/enviar audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean reproducirAudio(File archivoAudio) {
        // ... (Este método se mantiene igual, no necesita cambios) ...
        try {
            if (!archivoAudio.exists() || archivoAudio.length() == 0) {
                System.err.println("El archivo de audio no existe o está vacío");
                return false;
            }

            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(archivoAudio)) {
                AudioFormat format = audioStream.getFormat();

                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);

                clip.start();

                System.out.println("Duración: " + (clip.getMicrosecondLength() / 1000000.0) + " segundos");

                while (clip.isRunning()) {
                    Thread.sleep(100);
                }

                clip.close();
                return true;

            } catch (UnsupportedAudioFileException e) {
                System.err.println("Formato de audio no soportado: " + e.getMessage());
                return false;
            } catch (LineUnavailableException e) {
                System.err.println("Línea de audio no disponible: " + e.getMessage());
                return false;
            }

        } catch (Exception e) {
            System.err.println("Error al reproducir audio: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}