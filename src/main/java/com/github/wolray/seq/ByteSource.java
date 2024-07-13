package com.github.wolray.seq;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.UnaryOperator;

/**
 * @author wolray
 */
public interface ByteSource extends IOChain.Closable<InputStream> {
    static ByteSource of(InputStream is) {
        return () -> is;
    }

    static ByteSource of(URL url) {
        return url::openStream;
    }

    static ByteSource of(File file) {
        return of(file.toPath());
    }

    static ByteSource of(Path path) {
        return new ByteSource() {
            @Override
            public InputStream call() throws IOException {
                return Files.newInputStream(path);
            }

            @Override
            public byte[] toBytes() {
                return IOChain.apply(path, Files::readAllBytes);
            }

            @Override
            public Path write(Path target) {
                if (!path.equals(target)) {
                    IOChain.apply(path, p -> Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING));
                }
                return target;
            }

            @Override
            public IOChain<BufferedReader> toReader() {
                return (Closable<BufferedReader>)() -> Files.newBufferedReader(path, charset());
            }
        };
    }

    static ByteSource of(byte[] bytes) {
        return new ByteSource() {
            @Override
            public InputStream call() {
                return new ByteArrayInputStream(bytes);
            }

            @Override
            public byte[] toBytes() {
                return bytes;
            }

            @Override
            public ByteSource cache() {
                return this;
            }
        };
    }

    static ByteSource of(Iterable<String> iterable) {
        return of(iterable, "\n");
    }

    static ByteSource of(Iterable<String> iterable, String separator) {
        return () -> ItrUtil.toInputStream(iterable.iterator(), separator);
    }

    static ByteSource ofArray(IOChain<byte[]> bytes) {
        return of(bytes.get());
    }

    static ByteSource ofPath(IOChain<Path> path) {
        return of(path.get());
    }

    static ByteSource ofResource(String resource) {
        return ofResource(ByteSource.class, resource);
    }

    static ByteSource ofResource(Class<?> cls, String resource) {
        return IOChain.apply(cls.getResource(resource), url -> {
            try {
                if (url == null) {
                    throw new FileNotFoundException(resource);
                }
                return of(Paths.get(url.toURI()));
            } catch (URISyntaxException | FileSystemNotFoundException e) {
                return of(url);
            }
        });
    }

    static ByteSource ofStream(IOChain<InputStream> is) {
        return is::call;
    }

    static ByteSource ofUrl(String url) {
        return IOChain.apply(url, u -> of(new URL(u)));
    }

    default String asString() {
        return new String(toBytes(), charset());
    }

    default ByteSource cache() {
        return of(toBytes());
    }

    default Charset charset() {
        return Charset.defaultCharset();
    }

    default byte[] toBytes() {
        return toBytes(8192);
    }

    default byte[] toBytes(int bufferSize) {
        byte[] buff = new byte[bufferSize];
        List<byte[]> list = new ArrayList<>();
        int[] total = {0};
        use(is -> {
            int len;
            while ((len = is.read(buff, 0, buff.length)) > 0) {
                list.add(Arrays.copyOf(buff, len));
                total[0] += len;
            }
        });
        byte[] res = new byte[total[0]];
        int pos = 0;
        for (byte[] bytes : list) {
            System.arraycopy(bytes, 0, res, pos, bytes.length);
            pos += bytes.length;
        }
        return res;
    }

    default IOChain<Properties> toProperties() {
        return toReader().map(r -> {
            Properties p = new Properties();
            p.load(r);
            return p;
        });
    }

    default IOChain<BufferedReader> toReader() {
        return mapClosable(is -> new BufferedReader(new InputStreamReader(is, charset())));
    }

    default Seq<String> toSeq() {
        return toReader().toSeq(BufferedReader::readLine);
    }

    default Seq<String> toSeq(int skip) {
        return toReader().toSeq(BufferedReader::readLine, skip);
    }

    default Seq<String> toSeq(int n, UnaryOperator<String> replace) {
        return toReader().toSeq(BufferedReader::readLine, n, replace);
    }

    default ByteSource withCharset(Charset charset) {
        ByteSource origin = this;
        return new ByteSource() {
            @Override
            public InputStream call() throws IOException {
                return origin.call();
            }

            @Override
            public byte[] toBytes() {
                return origin.toBytes();
            }

            @Override
            public Path write(Path target) {
                return origin.write(target);
            }

            @Override
            public Charset charset() {
                return charset;
            }
        };
    }

    default Path write(Path target) {
        use(is -> Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING));
        return target;
    }

    default Path writeTemp(String suffix) {
        return IOChain.of(() -> write(Files.createTempFile("", suffix))).get();
    }
}
