package gov.cabinetofice.gapuserservice.util;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class PaginationUtils {

    public static <T> Page<T> paginateList(List<T> list, Pageable pageable) {
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;

        List<T> pagedItems;

        if (list.size() < startItem) {
            pagedItems = List.of();
        } else {
            int toIndex = Math.min(startItem + pageSize, list.size());
            pagedItems = list.subList(startItem, toIndex);
        }

        return new PageImpl<>(pagedItems, pageable, list.size());
    }
}