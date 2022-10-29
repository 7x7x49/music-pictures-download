import javazoom.jl.player.Player;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

public class Main {

    public static final String INPUT_FILE = "input.txt"; // файл с картинками и музыкой которую нужно скачвать
    private static final Queue<String> music = new PriorityQueue<>(); // очередь прослушивания

    public static void main(String[] args) {
        List<ThreadDownloader> threads = new ArrayList<>(); // список потоков (параллельное скачивание)
        try (BufferedReader br = new BufferedReader(new FileReader(INPUT_FILE))) { // читаем файл с картинками и музыкой
            String line;
            while ((line = br.readLine()) != null) { // проходимся по каждой строке
                String[] array = line.split("_"); // разбиваем по нижнему подчеркиванию
                if (array.length < 2) continue; // если в строке не содержалось символа _, пропускаем ее
                ThreadDownloader td = new ThreadDownloader(array[0], array[1]); // создаем поток скачивания
                td.start(); // запускаем его
                threads.add(td); // добавляем в список
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Player lastPlayer = null; // последняя запущенная музыка
        while (threads.stream().anyMatch(Thread::isAlive) || music.size() > 0) { // пока скачивается хотя бы 1 файл и очередь треков не пуста, программа будет работать
            if (music.size() > 0 && (lastPlayer == null || lastPlayer.isComplete())) { // если очередь треков не пуста и последняя запущенная музыка уже проиграна
                try {
                    String track = music.poll(); // получаем путь до новой музыки, которую нужно запустить
                    System.out.println("Включаю " + track + "..."); // говорим в консоль
                    lastPlayer = new Player(new FileInputStream(track)); // создаем плеер
                    lastPlayer.play(); // и запускаем музыку
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private static class ThreadDownloader extends Thread {

        private final String link, output; // ссылка и файл, куда он скачается
        private long startTime; // когда был запущен поток

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
        public void run() { // логика скачивания
            try {
                URL url = new URL(link);
                ReadableByteChannel byteChannel = Channels.newChannel(url.openStream());
                File outputFile = new File(output);
                File parent = outputFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs(); // если папки, в которой должен лежать скачанный файл не существует, то мы создаем ее
                FileOutputStream stream = new FileOutputStream(output);
                stream.getChannel().transferFrom(byteChannel, 0, Long.MAX_VALUE);
                stream.close();
                byteChannel.close();
                System.out.printf("Файл %s скачан за %.2f секунд\n", output, (System.currentTimeMillis() - startTime) / 1000f);
                if (output.endsWith(".mp3")) {
                    System.out.println("Обнаружена музыка! Добавление в очередь прослушивания...");
                    music.add(output); // добавляем в очередь прослушивания музыку
                }
            } catch (Exception e) {
                System.out.println("Произошла ошибка при скачивании файла " + link);
                e.printStackTrace();
            }

        }
    }
}