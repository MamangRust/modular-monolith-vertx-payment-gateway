package io.example.saldo.service;

import java.util.List;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.saldo.model.SaldoMonthBalanceResponse;
import io.example.saldo.model.SaldoMonthTotalBalanceResponse;
import io.example.saldo.model.SaldoResponse;
import io.example.saldo.model.SaldoResponseDeleteAt;
import io.example.saldo.model.SaldoYearBalanceResponse;
import io.example.saldo.model.SaldoYearTotalBalanceResponse;
import io.vertx.core.Future;
import pb.saldo.Saldo.FindAllSaldoRequest;
import io.example.saldo.domain.requests.MonthTotalSaldoBalance;

public interface SaldoQueryService {
  Future<ApiResponsePagination<List<SaldoResponse>>> getAllSaldos(FindAllSaldoRequest req);
  Future<ApiResponsePagination<List<SaldoResponseDeleteAt>>> getActiveSaldos(FindAllSaldoRequest req);
  Future<ApiResponsePagination<List<SaldoResponseDeleteAt>>> getTrashedSaldos(FindAllSaldoRequest req);
  Future<ApiResponse<SaldoResponse>> getSaldoByCardNumber(String cardNumber);
  Future<ApiResponse<SaldoResponse>> getSaldoById(Integer saldoId);
  Future<ApiResponse<List<SaldoMonthTotalBalanceResponse>>> getMonthlyTotalSaldoBalance(MonthTotalSaldoBalance req);
  Future<ApiResponse<List<SaldoYearTotalBalanceResponse>>> getYearlyTotalSaldoBalances(Integer endYear);
  Future<ApiResponse<List<SaldoMonthBalanceResponse>>> getMonthlySaldoBalances(Integer year);
  Future<ApiResponse<List<SaldoYearBalanceResponse>>> getYearlySaldoBalances(Integer endYear);
}
