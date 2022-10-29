import javazoom.jl.player.Player;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

public class Main {

    public static final String INPUT_FILE = "input.txt";
    private static final Queue<String> music = new PriorityQueue<>();

    public static void main(String[] args) {
        List<ThreadDownloader> threads = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(INPUT_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] array = line.split("_");
                if (array.length < 2) continue;
                ThreadDownloader td = new ThreadDownloader(array[0], array[1]);
                td.start();
                threads.add(td);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Player lastPlayer = null;
        while (threads.stream().anyMatch(Thread::isAlive) || music.size() > 0) {
            if (music.size() > 0 && (lastPlayer == null || lastPlayer.isComplete())) {
                try {
                    String track = music.poll();
                    System.out.println("Включаю " + track + "...");
                    lastPlayer = new Player(new FileInputStream(track));
                    lastPlayer.play();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static class ThreadDownloader extends Thread {

        private final String link, output;
        private long startTime;

        private ThreadDownloader(String link, String output) {
            this.link = link;
            this.output = output;
        }

        @Override
        public synchronized void start() {
            super.start();
            System.out.println("Скачивание файла " + link);
            startTime = System.currentTimeMillis();
        }

        @Override
        public void run() {
            try {
                URL url = new URL(link);
                ReadableByteChannel byteChannel = Channels.newChannel(url.openStream());
                File outputFile = new File(output);
                File parent = outputFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                FileOutputStream stream = new FileOutputStream(output);
                stream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);
                stream.close();
                byteChannel.close();
                System.out.printf("Файл %s скачан за %.2f секунд\n", output, (System.currentTimeMillis() - startTime) / 1000f);
                if (output.endsWith(".mp3")) {
                    System.out.println("Обнаружена музыка! Добавление в очередь прослушивания...");
                    music.add(output);
                }
            } catch (Exception e) {
                System.out.println("Произошла ошибка при скачивании файла " + link);
                e.printStackTrace();
            }

        }
    }
}