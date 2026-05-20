package io.example.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PagedResult<T> {
  private List<T> data;
  private int totalRecords;
}
