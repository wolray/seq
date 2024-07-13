package com.github.wolray.seq;

import java.io.InputStream;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author wolray
 */
public interface ItrUtil {
    static <T> Iterator<T> drop(Iterator<T> iterator, int n) {
        return n <= 0 ? iterator : new PickItr<T>() {
            int i = n;

            @Override
            public T pick() {
                for (; i > 0; i--) {
                    pop(iterator);
                }
                return pop(iterator);
            }
        };
    }

    static <T> Iterator<T> dropWhile(Iterator<T> iterator, Predicate<T> predicate) {
        return new PickItr<T>() {
            boolean flag = true;

            @Override
            public T pick() {
                T t = pop(iterator);
                if (flag) {
                    while (predicate.test(t)) {
                        t = pop(iterator);
                    }
                    flag = false;
                }
                return t;
            }
        };
    }

    static <T> Iterator<T> filter(Iterator<T> iterator, Predicate<T> predicate) {
        return new PickItr<T>() {
            @Override
            public T pick() {
                while (iterator.hasNext()) {
                    T t = iterator.next();
                    if (predicate.test(t)) {
                        return t;
                    }
                }
                return Seq.stop();
            }
        };
    }

    static <T> PickItr<T> flat(Iterator<? extends Iterable<T>> iterator) {
        return new PickItr<T>() {
            Iterator<T> cur = Collections.emptyIterator();

            @Override
            public T pick() {
                while (!cur.hasNext()) {
                    cur = pop(iterator).iterator();
                }
                return cur.next();
            }
        };
    }

    static <T, E> PickItr<E> flat(Iterator<T> iterator, Function<T, ? extends Iterable<E>> function) {
        return flat(map(iterator, function));
    }

    static <T> PickItr<T> flatOptional(Iterator<Optional<T>> iterator) {
        return new PickItr<T>() {
            @Override
            public T pick() {
                while (iterator.hasNext()) {
                    Optional<T> opt = iterator.next();
                    if (opt.isPresent()) {
                        return opt.get();
                    }
                }
                return Seq.stop();
            }
        };
    }

    static <T, E> Iterator<E> map(Iterator<T> iterator, Function<T, E> function) {
        return new MapItr<T, E>(iterator) {
            @Override
            public E apply(T t) {
                return function.apply(t);
            }
        };
    }

    static <T, E> Iterator<E> map(Iterator<T> iterator, Function<T, E> function, int n, Function<T, E> substitute) {
        return new MapItr<T, E>(iterator) {
            int i = n - 1;

            @Override
            public E apply(T t) {
                if (i < 0) {
                    return function.apply(t);
                } else {
                    i--;
                    return substitute.apply(t);
                }
            }
        };
    }

    static <T, E> Iterator<E> mapIndexed(Iterator<T> iterator, Seq.IndexObjFunction<T, E> function) {
        return new MapItr<T, E>(iterator) {
            int i = 0;

            @Override
            public E apply(T t) {
                return function.apply(i++, t);
            }
        };
    }

    static <T> T pop(Iterator<T> iterator) {
        return iterator.hasNext() ? iterator.next() : Seq.stop();
    }

    static <T> Iterator<T> take(Iterator<T> iterator, int n) {
        return n <= 0 ? Collections.emptyIterator() : new PickItr<T>() {
            int i = n;

            @Override
            public T pick() {
                return i-- > 0 ? pop(iterator) : Seq.stop();
            }
        };
    }

    static <T> Iterator<T> takeWhile(Iterator<T> iterator, Predicate<T> predicate) {
        return new PickItr<T>() {
            @Override
            public T pick() {
                T t = pop(iterator);
                return predicate.test(t) ? t : Seq.stop();
            }
        };
    }

    static <T, E> Iterator<T> takeWhile(Iterator<T> iterator, Function<T, E> function, BiPredicate<E, E> testPrevCurr) {
        return new PickItr<T>() {
            E last = null;

            @Override
            public T pick() {
                T t = pop(iterator);
                E curr = function.apply(t);
                if (last == null || testPrevCurr.test(last, curr)) {
                    last = curr;
                    return t;
                } else {
                    return Seq.stop();
                }
            }
        };
    }

    static InputStream toInputStream(Iterable<String> iterable, String separator) {
        return toInputStream(iterable.iterator(), separator);
    }

    static InputStream toInputStream(Iterator<String> iterator, String separator) {
        return new InputItr(iterator, separator);
    }

    static <T> Stream<T> toStream(Iterator<T> iterator) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
            false);
    }

    static <T> Iterator<T> zip(Iterator<T> iterator, T t) {
        return new PickItr<T>() {
            boolean flag = false;

            @Override
            public T pick() {
                flag = !flag;
                return flag ? pop(iterator) : t;
            }
        };
    }

    class InputItr extends InputStream {
        final Iterator<byte[]> iterator;
        byte[] cur = {};
        int i;

        public InputItr(Iterator<String> itr, String sep) {
            Iterator<byte[]> bytesIterator = map(itr, String::getBytes);
            iterator = sep.isEmpty() ? bytesIterator : zip(bytesIterator, sep.getBytes());
        }

        @Override
        public int read() {
            if (i < cur.length) {
                return cur[i++] & 0xFF;
            }
            i = 0;
            while (iterator.hasNext()) {
                cur = iterator.next();
                if (cur.length > 0) {
                    return cur[i++] & 0xFF;
                }
            }
            return -1;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            int srcRest = cur.length - i;
            if (srcRest >= len) {
                System.arraycopy(cur, i, b, off, len);
                i += len;
                return len;
            } else {
                int count = 0;
                if (srcRest > 0) {
                    System.arraycopy(cur, i, b, off, srcRest);
                    off += srcRest;
                    count += srcRest;
                    i = cur.length;
                }
                while (count < len && iterator.hasNext()) {
                    byte[] bytes = iterator.next();
                    if (bytes.length > 0) {
                        int desRest = len - count;
                        if (bytes.length >= desRest) {
                            System.arraycopy(cur = bytes, 0, b, off, desRest);
                            i = desRest;
                            count = len;
                        } else {
                            System.arraycopy(bytes, 0, b, off, bytes.length);
                            off += bytes.length;
                            count += bytes.length;
                        }
                    }
                }
                return count > 0 ? count : -1;
            }
        }
    }
}
