package rop;

import java.util.Objects;

/**
 * A generic class to be used as the result of an operation.
 * Allows to return both a Success and a Failure value at
 * the same time, eliminating the need for most exceptions.
 * 
 * All instances of this class are immutable.
 * 
 * @author Philip Klostermann
 *
 * @param <S> - Type of the success object
 * @param <F> - TYpe of the failure object
 */

public class Result<S, F> {
  public final boolean SUCCESS;
  public final boolean FAILURE;
  public final S success;
  public final F failure;

  public Result(boolean SUCCESS, boolean FAILURE, S success, F failure) {
    if (SUCCESS && FAILURE) {
      throw new RuntimeException("A result cannot be both a success and a failure");
    }
    
    if (!SUCCESS && !FAILURE) {
      throw new RuntimeException("A result must be either a success or a failure");
    }
    
    if (SUCCESS && success == null) {
      throw new RuntimeException("A success result needs to have a success payload");
    }
    
    if (FAILURE && failure == null) {
      throw new RuntimeException("A failure result needs to have a failure payload");
    }
    
    if (success != null && failure != null) {
      throw new RuntimeException("A result cannot have both, a success and failure payload");
    }
    
    this.SUCCESS = SUCCESS;
    this.FAILURE = FAILURE;
    this.success = success;
    this.failure = failure;
  }

  /**
   * Shorthand constructor that builds a success result
   * @param data
   */
  public Result(S data) {
    Objects.requireNonNull(data);
    this.SUCCESS = true;
    this.FAILURE = false;
    this.success = data;
    this.failure = null;
  }

  /**
   * Convenience method to quickly create a success result.
   * CAREFUL: Success and Failure types are not checked here.
   * @param data
   * @return instance of Result with success
   */
  public static <S, F> Result<S, F> success(S data) {
    Objects.requireNonNull(data);
    return new Result<S, F>(data);
  }

  /**
   * Convenience method to quickly create a failure result.
   * CAREFUL: Success and Failure types are not checked here.
   * @param error
   * @return instance of Result with failure
   */
  public static <S, F> Result<S, F> failure(F error) {
    Objects.requireNonNull(error);
    return new Result<S, F>(false, true, null, error);
  }
}