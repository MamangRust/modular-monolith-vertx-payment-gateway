package io.example.saldo.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.saldo.service.SaldoQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.saldo.Saldo.ApiResponseSaldo;
import pb.saldo.Saldo.FindAllSaldoRequest;
import pb.saldo.Saldo.FindByIdSaldoRequest;
import pb.saldo.SaldoQuery.ApiResponsePaginationSaldo;
import pb.saldo.SaldoQuery.ApiResponsePaginationSaldoDeleteAt;

@RequiredArgsConstructor
public class SaldoQueryHandler implements pb.saldo.VertxSaldoQueryServiceGrpcServer.SaldoQueryServiceApi {
  private final SaldoQueryService service;

  private pb.common.PaginationMeta buildPaginationMeta(int page, int pageSize, int totalRecords) {
    int safePage = page > 0 ? page : 1;
    int safePageSize = pageSize > 0 ? pageSize : 10;
    int totalPages = (int) Math.ceil((double) totalRecords / safePageSize);
    return pb.common.PaginationMeta.newBuilder()
        .setCurrentPage(safePage)
        .setPageSize(safePageSize)
        .setTotalPages(totalPages)
        .setTotalRecords(totalRecords)
        .build();
  }

  @Override
  public Future<ApiResponsePaginationSaldo> findAllSaldo(FindAllSaldoRequest req) {
    return service.getAllSaldos(req)
        .map(resp -> {
          var builder = ApiResponsePaginationSaldo.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::fromSaldoResponse).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseSaldo> findByIdSaldo(FindByIdSaldoRequest req) {
    return service.getSaldoById(req.getSaldoId())
        .map(data -> ApiResponseSaldo.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromSaldoResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseSaldo> findByCardNumber(pb.card.Card.FindByCardNumberRequest req) {
    return service.getSaldoByCardNumber(req.getCardNumber())
        .map(data -> ApiResponseSaldo.newBuilder()
            .setStatus("success")
            .setMessage("OK")
            .setData(ProtoConverter.fromSaldoResponse(data))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationSaldoDeleteAt> findByActive(FindAllSaldoRequest req) {
    return service.getActiveSaldos(req)
        .map(resp -> {
          var builder = ApiResponsePaginationSaldoDeleteAt.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::fromSaldoResponseDeleteAt).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationSaldoDeleteAt> findByTrashed(FindAllSaldoRequest req) {
    return service.getTrashedSaldos(req)
        .map(resp -> {
          var builder = ApiResponsePaginationSaldoDeleteAt.newBuilder()
              .setStatus("success")
              .setMessage("OK")
              .setPaginationMeta(buildPaginationMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()));
          resp.getData().stream().map(ProtoConverter::fromSaldoResponseDeleteAt).forEach(builder::addData);
          return builder.build();
        })
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}