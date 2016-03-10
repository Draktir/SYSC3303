package rop;

import java.util.Objects;

public class Result<S, F> {
  public final boolean SUCCESS;
  public final boolean FAILURE;
  public final S success;
  public final F failure;

  public Result(S data) {
    Objects.requireNonNull(data);
    this.SUCCESS = true;
    this.FAILURE = false;
    this.success = data;
    this.failure = null;
  }

  public Result(boolean success, F error) {
    Objects.requireNonNull(error);
    this.SUCCESS = false;
    this.FAILURE = true;
    this.success = null;
    this.failure = error;
  }

  public static <S, F> Result<S, F> success(S data) {
    Objects.requireNonNull(data);
    return new Result<S, F>(data);
  }

  public static <S, F> Result<S, F> failure(F error) {
    Objects.requireNonNull(error);
    return new Result<S, F>(false, error);
  }
}