package vn.hoadon.dto;

import java.util.List;

public class PageResponseDTO<T> {

    private List<T> data;
    private long total;
    private int page;
    private int lastPage;

    public PageResponseDTO() {}

    public PageResponseDTO(List<T> data, long total, int page, int lastPage) {
        this.data = data;
        this.total = total;
        this.page = page;
        this.lastPage = lastPage;
    }

    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getLastPage() { return lastPage; }
    public void setLastPage(int lastPage) { this.lastPage = lastPage; }
}
