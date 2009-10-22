package groovy.util;

import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport;

import java.util.*;

import groovy.lang.Closure;

public abstract class TransformClosure<T,R> extends DefaultGroovyMethodsSupport {
    public abstract R transform(T element);

    public static <T,R> Collection<R> transform(Collection<T> self, TransformClosure<T,R> transform) {
        Collection<R> res = (Collection<R>) createSimilarCollection(self);
        for (T t : self) {
            res.add(transform.transform(t));
        }
        return res;
    }

    public static <T,R> R[] transform(T[] self, TransformClosure<T,R> transform) {
        R [] res = (R[]) new Object[self.length];
        for (int i = 0; i < res.length; i++) {
            res [i] = transform.transform(self[i]);
        }
        return res;
    }

    public static <T,R> R transform(T self, TransformClosure<T,R> transform) {
        return transform.transform(self);
    }

    public static <T,R> Iterator<R> transform(final Iterator<T> self, final TransformClosure<T,R> transform) {
        return new Iterator<R> () {
            public boolean hasNext() {
                return self.hasNext();
            }

            public R next() {
                return transform.transform(self.next());
            }

            public void remove() {
                self.remove();
            }
        };
    }

    public static <T,K> Map<K, List<T>> groupBy(Collection<T> self, TransformClosure<T,K> transform) {
        Map<K, List<T>> answer = new LinkedHashMap<K, List<T>>();
        for (T element : self) {
            K value = transform.transform(element);
            List<T> list = answer.get(value);
            if (list == null) {
                list = new LinkedList<T> ();
                answer.put(value, list);
            }
            list.add(element);
        }
        return answer;
    }
}
