package com.brava.memories.upload;

import com.brava.memories.config.VideoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Re-encodes an uploaded video to a universally-playable H.264/AAC MP4 rendition and extracts a
 * poster frame, by shelling out to the system ffmpeg binary. The original file is never touched —
 * this only ever produces additional derived copies alongside it. Best-effort like ThumbnailService:
 * any failure (ffmpeg missing, a timeout, an input ffmpeg can't decode) just means no rendition or
 * poster is stored, and playback/download falls back to the original file.
 */
@Service
public class VideoTranscodingService {
    private static final Logger log = LoggerFactory.getLogger(VideoTranscodingService.class);
    private static final int POSTER_WIDTH = 640;

    private final VideoProperties props;
    private final boolean ffmpegAvailable;

    public VideoTranscodingService(VideoProperties props) {
        this.props = props;
        this.ffmpegAvailable = props.transcodingEnabled() && checkFfmpegAvailable();
        if (props.transcodingEnabled() && !ffmpegAvailable) {
            log.warn("Video transcoding is enabled but the ffmpeg binary ('{}') was not found or did not respond; video renditions and poster frames will be skipped.", props.ffmpegPath());
        }
    }

    private boolean checkFfmpegAvailable() {
        try {
            Process p = new ProcessBuilder(props.ffmpegPath(), "-version").redirectErrorStream(true).start();
            drainAsync(p);
            boolean finished = p.waitFor(10, TimeUnit.SECONDS);
            if (!finished) { p.destroyForcibly(); return false; }
            return p.exitValue() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    public byte[] transcodeToH264(InputStream video) {
        if (!ffmpegAvailable) return null;
        Path input = null;
        try {
            input = materializeInput(video);
            return runFfmpegOnFile(input, ".mp4",
                    "-c:v", "libx264", "-profile:v", "high", "-level", "4.0", "-pix_fmt", "yuv420p",
                    "-preset", "veryfast", "-crf", "23",
                    "-c:a", "aac", "-b:a", "128k",
                    "-movflags", "+faststart");
        } catch (Exception ex) {
            log.warn("Video processing failed: {}", ex.getMessage());
            return null;
        } finally {
            deleteQuietly(input);
        }
    }

    public byte[] extractPosterFrame(InputStream video) {
        if (!ffmpegAvailable) return null;
        Path input = null;
        try {
            input = materializeInput(video);
            // Skip the first half-second (often a black/blank frame on some cameras); a clip shorter
            // than that produces an empty output, so fall back to the very first frame — always safe.
            byte[] frame = runFfmpegOnFile(input, ".jpg", "-ss", "00:00:00.5", "-vframes", "1", "-update", "1", "-vf", "scale=" + POSTER_WIDTH + ":-1");
            if (frame != null) return frame;
            return runFfmpegOnFile(input, ".jpg", "-vframes", "1", "-update", "1", "-vf", "scale=" + POSTER_WIDTH + ":-1");
        } catch (Exception ex) {
            log.warn("Video processing failed: {}", ex.getMessage());
            return null;
        } finally {
            deleteQuietly(input);
        }
    }

    private Path materializeInput(InputStream video) throws IOException {
        Path input = Files.createTempFile("video-in-", ".bin");
        Files.copy(video, input, StandardCopyOption.REPLACE_EXISTING);
        return input;
    }

    private byte[] runFfmpegOnFile(Path input, String outputSuffix, String... args) {
        Path output = null;
        try {
            output = Files.createTempFile("video-out-", outputSuffix);

            List<String> command = new ArrayList<>();
            command.add(props.ffmpegPath());
            command.add("-y");
            command.add("-i");
            command.add(input.toString());
            command.addAll(List.of(args));
            command.add(output.toString());

            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            DrainedOutput drained = drainAsync(process);
            boolean finished = process.waitFor(props.timeout().toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("ffmpeg timed out after {}", props.timeout());
                return null;
            }
            drained.join();
            if (process.exitValue() != 0) {
                log.warn("ffmpeg exited with {}: {}", process.exitValue(), drained.lastLine());
                return null;
            }
            long size = Files.size(output);
            return size == 0 ? null : Files.readAllBytes(output);
        } catch (Exception ex) {
            log.warn("Video processing failed: {}", ex.getMessage());
            return null;
        } finally {
            deleteQuietly(output);
        }
    }

    /** Must drain the process's stdout concurrently with waiting for it — otherwise a chatty ffmpeg
     *  can fill the OS pipe buffer and deadlock, since it'd block writing while we block on waitFor. */
    private DrainedOutput drainAsync(Process process) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Thread thread = new Thread(() -> {
            try (InputStream out = process.getInputStream()) { out.transferTo(buffer); } catch (IOException ignored) {}
        });
        thread.setDaemon(true);
        thread.start();
        return new DrainedOutput(thread, buffer);
    }

    private record DrainedOutput(Thread thread, ByteArrayOutputStream buffer) {
        void join() { try { thread.join(5000); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); } }
        String lastLine() {
            String text = buffer.toString(StandardCharsets.UTF_8);
            String[] lines = text.split("\\R");
            return lines.length == 0 ? "" : lines[lines.length - 1];
        }
    }

    private void deleteQuietly(Path p) {
        if (p == null) return;
        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
    }
}
