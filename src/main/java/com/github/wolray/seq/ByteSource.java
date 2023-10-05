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
public interface ByteSource {
    byte[] toBytes();
    InputStream toInputStream();

    static ArraySource of(byte[] bytes) {
        return () -> bytes;
    }

    static PathSource of(File file) {
        return of(file.toPath());
    }

    static ISSource of(InputStream is) {
        return () -> is;
    }

    static ByteSource of(Iterable<String> iterable) {
        return of(iterable, "\n");
    }

    static ISSource of(Iterable<String> iterable, String separator) {
        return () -> ItrUtil.toInputStream(iterable.iterator(), separator);
    }

    static PathSource of(Path path) {
        return () -> path;
    }

    static PathSource of(String file) {
        return of(Paths.get(file));
    }

    static ISSource of(URL url) {
        return url::openStream;
    }

    static ArraySource ofArray(IOChain<byte[]> bytes) {
        return bytes::call;
    }

    static PathSource ofPath(IOChain<Path> path) {
        return path::call;
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

    static ByteSource ofResource(String resource) {
        return ofResource(ByteSource.class, resource);
    }

    static ISSource ofStream(IOChain<InputStream> is) {
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

    default IOChain<Properties> toProperties() {
        return toReader().map(r -> {
            Properties p = new Properties();
            p.load(r);
            return p;
        });
    }

    default IOChain<BufferedReader> toReader() {
        return IOChain.ofReader(() -> new InputStreamReader(toInputStream(), charset()));
    }

    default Seq<String> toSeq() {
        IOChain<BufferedReader> chain = toReader();
        return c -> chain.use(reader -> {
            String s;
            while ((s = reader.readLine()) != null) {
                c.accept(s);
            }
        });
    }

    default Seq<String> toSeq(int n, UnaryOperator<String> replace) {
        if (n <= 0 || replace == null) {
            return toSeq();
        }
        IOChain<BufferedReader> chain = toReader();
        return c -> chain.use(reader -> {
            String s;
            for (int i = 0; i < n; i++) {
                s = reader.readLine();
                if (s == null) {
                    return;
                }
                c.accept(replace.apply(s));
            }
            while ((s = reader.readLine()) != null) {
                c.accept(s);
            }
        });
    }

    default ByteSource withCharset(Charset charset) {
        ByteSource origin = this;
        return new ByteSource() {
            @Override
            public byte[] toBytes() {
                return origin.toBytes();
            }

            @Override
            public InputStream toInputStream() {
                return origin.toInputStream();
            }

            @Override
            public Charset charset() {
                return charset;
            }
        };
    }

    default Path write(Path target) {
        IOChain.apply(toInputStream(), is -> Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING));
        return target;
    }

    default Path writeTemp(String suffix) {
        return IOChain.of(() -> write(Files.createTempFile("", suffix))).get();
    }

    interface ArraySource extends ByteSource, IOChain<byte[]> {
        @Override
        default byte[] toBytes() {
            return get();
        }

        @Override
        default InputStream toInputStream() {
            return new ByteArrayInputStream(get());
        }

        @Override
        default ByteSource cache() {
            return this;
        }
    }

    interface ISSource extends ByteSource, IOChain<InputStream> {
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

        @Override
        default byte[] toBytes() {
            return toBytes(8192);
        }

        @Override
        default InputStream toInputStream() {
            return get();
        }
    }

    interface PathSource extends ByteSource, IOChain<Path> {
        @Override
        default byte[] toBytes() {
            return apply(Files::readAllBytes);
        }

        @Override
        default InputStream toInputStream() {
            return apply(Files::newInputStream);
        }

        @Override
        default Path write(Path target) {
            use(path -> {
                if (!path.equals(target)) {
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            });
            return target;
        }

        @Override
        default IOChain<BufferedReader> toReader() {
            return (Closable<BufferedReader>)() -> Files.newBufferedReader(call(), charset());
        }
    }
}
