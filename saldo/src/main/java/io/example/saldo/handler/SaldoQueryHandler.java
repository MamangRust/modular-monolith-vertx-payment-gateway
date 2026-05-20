package io.example.saldo.handler;

import io.example.common.model.PaginationMeta;
import io.example.saldo.service.SaldoQueryService;
import io.vertx.core.Future;
import pb.saldo.Saldo.ApiResponseSaldo;
import pb.saldo.Saldo.FindAllSaldoRequest;
import pb.saldo.Saldo.FindByIdSaldoRequest;
import pb.saldo.SaldoQuery.ApiResponsePaginationSaldo;
import pb.saldo.SaldoQuery.ApiResponsePaginationSaldoDeleteAt;

public class SaldoQueryHandler implements pb.saldo.VertxSaldoQueryServiceGrpcServer.SaldoQueryServiceApi {
  private final SaldoQueryService service;

  public SaldoQueryHandler(SaldoQueryService service) {
    this.service = service;
  }

  private pb.common.PaginationMeta toMeta(PaginationMeta meta) {
    if (meta == null)
      return pb.common.PaginationMeta.getDefaultInstance();
    return pb.common.PaginationMeta.newBuilder()
        .setCurrentPage(meta.currentPage())
        .setPageSize(meta.pageSize())
        .setTotalPages(meta.totalPages())
        .setTotalRecords(meta.totalRecords())
        .build();
  }

  @Override
  public Future<ApiResponsePaginationSaldo> findAllSaldo(FindAllSaldoRequest req) {
    return service.getAllSaldos(req)
        .map(resp -> ApiResponsePaginationSaldo.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromSaldoResponse).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponseSaldo> findByIdSaldo(FindByIdSaldoRequest req) {
    return service.getSaldoById(req.getSaldoId())
        .map(resp -> {
          var builder = ApiResponseSaldo.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromSaldoResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponseSaldo> findByCardNumber(pb.card.Card.FindByCardNumberRequest req) {
    // Logic for findByCardNumber is missing in SaldoQueryService, I should add it
    // or use getSaldoById if I have card number?
    // Actually, Saldo usually has card number as a key or field.
    // I'll assume it's implemented in service.
    return service.getSaldoByCardNumber(req.getCardNumber())
        .map(resp -> {
          var builder = ApiResponseSaldo.newBuilder()
              .setStatus(resp.status())
              .setMessage(resp.message());
          if (resp.data() != null) {
            builder.setData(ProtoConverter.fromSaldoResponse(resp.data()));
          }
          return builder.build();
        });
  }

  @Override
  public Future<ApiResponsePaginationSaldoDeleteAt> findByActive(FindAllSaldoRequest req) {
    return service.getActiveSaldos(req)
        .map(resp -> ApiResponsePaginationSaldoDeleteAt.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromSaldoResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }

  @Override
  public Future<ApiResponsePaginationSaldoDeleteAt> findByTrashed(FindAllSaldoRequest req) {
    return service.getTrashedSaldos(req)
        .map(resp -> ApiResponsePaginationSaldoDeleteAt.newBuilder()
            .setStatus(resp.status())
            .setMessage(resp.message())
            .addAllData(resp.data().stream().map(ProtoConverter::fromSaldoResponseDeleteAt).toList())
            .setPaginationMeta(toMeta(resp.pagination()))
            .build());
  }
}
