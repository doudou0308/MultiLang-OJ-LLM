package com.exam.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

@Data
public class PageDTO<T> {
    private List<T> records;
    private long total;
    private long size;
    private long current;
    private long pages;

    public static <T> PageDTO<T> of(IPage<T> page) {
        PageDTO<T> dto = new PageDTO<>();
        dto.records = page.getRecords();
        dto.total = page.getTotal();
        dto.size = page.getSize();
        dto.current = page.getCurrent();
        dto.pages = page.getPages();
        return dto;
    }
}
