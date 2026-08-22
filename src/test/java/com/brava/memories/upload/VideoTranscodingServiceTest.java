package com.brava.memories.upload;

import com.brava.memories.config.VideoProperties;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real ffmpeg binary against a genuine synthetic HEVC test video (fixtures/sample-hevc.mp4,
 * generated with `ffmpeg -f lavfi -i testsrc ... -c:v libx265 ...`) — this is exactly the kind of file an
 * iPhone produces, not a mocked stand-in.
 */
class VideoTranscodingServiceTest {
    private final VideoTranscodingService service = new VideoTranscodingService(new VideoProperties(true, "ffmpeg", Duration.ofMinutes(2)));

    @Test
    void transcodesAGenuineHevcVideoToPlayableH264() throws Exception {
        byte[] rendition = service.transcodeToH264(sampleVideo());

        assertThat(rendition).isNotNull();
        assertThat(rendition.length).isGreaterThan(0);
        assertThat(probeCodec(rendition, "v:0")).isEqualTo("h264");
        assertThat(probePixelFormat(rendition)).isEqualTo("yuv420p");
    }

    @Test
    void extractsAPosterFrameFromAGenuineHevcVideo() throws Exception {
        byte[] poster = service.extractPosterFrame(sampleVideo());

        assertThat(poster).isNotNull();
        assertThat(poster.length).isGreaterThan(0);
        assertThat(new String(java.util.Arrays.copyOfRange(poster, 6, 10))).isEqualTo("JFIF");
    }

    @Test
    void gracefullyReturnsNullWhenFfmpegIsMisconfigured() {
        VideoTranscodingService broken = new VideoTranscodingService(new VideoProperties(true, "/no/such/ffmpeg-binary", Duration.ofSeconds(5)));
        assertThat(broken.transcodeToH264(sampleVideo())).isNull();
        assertThat(broken.extractPosterFrame(sampleVideo())).isNull();
    }

    private InputStream sampleVideo() {
        try {
            return new ByteArrayInputStream(Files.readAllBytes(java.nio.file.Path.of("src/test/resources/fixtures/sample-hevc.mp4")));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String probeCodec(byte[] video, String stream) throws Exception {
        return ffprobe(video, "stream=codec_name", stream);
    }

    private String probePixelFormat(byte[] video) throws Exception {
        return ffprobe(video, "stream=pix_fmt", "v:0");
    }

    private String ffprobe(byte[] video, String entries, String stream) throws Exception {
        var tmp = Files.createTempFile("probe-", ".mp4");
        try {
            Files.write(tmp, video);
            Process p = new ProcessBuilder("ffprobe", "-v", "error", "-select_streams", stream,
                    "-show_entries", entries, "-of", "csv=p=0", tmp.toString())
                    .redirectErrorStream(true).start();
            String output = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            return output;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
