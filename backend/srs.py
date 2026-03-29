def calculate_next_review(ease_factor, interval_days, repetition_count, quality):
    new_ef = ease_factor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02))
    new_ef = max(1.3, new_ef)

    if quality < 3:
        new_repetition = 0
        new_interval = 1
    else:
        new_repetition = repetition_count + 1
        if repetition_count == 0:
            new_interval = 1
        elif repetition_count == 1:
            new_interval = 6
        else:
            new_interval = round(interval_days * new_ef)

    status = "learning" if new_repetition < 3 else "review"

    return {
        "ease_factor": new_ef,
        "interval_days": new_interval,
        "repetition_count": new_repetition,
        "status": status,
    }