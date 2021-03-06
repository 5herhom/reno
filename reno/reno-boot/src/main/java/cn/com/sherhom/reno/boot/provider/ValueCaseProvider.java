package cn.com.sherhom.reno.boot.provider;
/**
 * @author Sherhom
 * @date 2020/9/3 20:12
 */
public class ValueCaseProvider<T> extends AbstractCaseProvider<T> implements CaseProvider<T> {

    T value;
    boolean visited=false;
    public ValueCaseProvider(T value) {
        this.value = value;
    }

    @Override
    public T next() {
        this.visited=true;
        return value;
    }

    @Override
    public boolean hasNext() {
        return !visited;
    }

    @Override
    public void reset() {
        this.visited=false;
    }
}
