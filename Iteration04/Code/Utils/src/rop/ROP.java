package rop;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/*
 * Attempt to (partially) implement the Railway Oriented paradigm for error handling.
 * Based on: http://fsharpforfunandprofit.com/posts/recipe-part2/
 */


public class ROP<I, S, F> {
  /* transforms a function that accepts a single argument (I) and returns a Result<S, F> (switch function)
   * into a function that accepts a Result<I, F> and returns a Result<S, F> (two-track function)
   */
  public Function<Result<I, F>, Result<S, F>> bind (Function<I, Result<S, F>> switchFunction) {
    Objects.requireNonNull(switchFunction);
    return (twoTrackInput) -> {
      if (twoTrackInput.SUCCESS) {
        return switchFunction.apply(twoTrackInput.success);
      } else {
        return Result.failure(twoTrackInput.failure);
      }
    };
  }
  
  /* transforms a simple function that takes an argument (I) and returns a value (S)
   * into a switch function that accepts I and return Result<S, F>. Will always succeed.
   */
  public Function<I, Result<S, F>> buildSwitch (Function<I, S> singleFunction) {
    Objects.requireNonNull(singleFunction);
    return (input) -> {
      return Result.success(singleFunction.apply(input));
    };
  }

  /* combine two switch functions (accept I and return Result<S, F>) into a single switch containing
   * both functions (accepts S and returns Result<S, F>).
   */
  public Function<I, Result<S, F>> combine (Function<I, Result<S, F>> s1, Function<S, Result<S, F>> s2) {
    Objects.requireNonNull(s1);
    Objects.requireNonNull(s2);
    return (x) -> {
      Result<S, F> result = s1.apply(x);
      if (result.SUCCESS) {
        return s2.apply(result.success);
      } else {
        return result;
      }
    };
  }

  /* transforms a single-track function (accepts I and returns S) into a two-track function
   * that accepts Result<I, F> and returns Result<S, F>. Will always Succeed.
   */
  public Function<Result<I, F>, Result<S, F>> map (Function<I, S> oneTrackFunction) {
    Objects.requireNonNull(oneTrackFunction);
    return (twoTrackInput) -> {
      if (twoTrackInput.SUCCESS) {
        S result = oneTrackFunction.apply(twoTrackInput.success);
        return new Result<>(result);
      } else {
        return Result.failure(twoTrackInput.failure);
      }
    };
  }

  /* transforms a dead-end, single-track function a.k.a. a Consumer (accepts I, returns nothing)
   * into a single-track function (accepts S, returns S).
   */
  public Function<S, S> tee (Consumer<S> deadEndFunction) {
    Objects.requireNonNull(deadEndFunction);
    return (x) -> {
      deadEndFunction.accept(x);
      return x;
    };
  }
}

