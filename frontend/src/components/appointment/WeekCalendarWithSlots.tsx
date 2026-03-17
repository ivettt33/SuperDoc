import React, { useState, useEffect } from "react";
import { HiChevronLeft, HiChevronRight } from "react-icons/hi";
import { AvailableTimeSlot } from "../../api";

interface WeekCalendarWithSlotsProps {
  selectedDate: Date | null;
  selectedTime: string | null;
  onDateSelect: (date: Date) => void;
  onTimeSelect: (time: string) => void;
  availableSlots: AvailableTimeSlot[];
  loading?: boolean;
}

export default function WeekCalendarWithSlots({
  selectedDate,
  selectedTime,
  onDateSelect,
  onTimeSelect,
  availableSlots,
  loading = false,
}: WeekCalendarWithSlotsProps) {
  const [currentWeekStart, setCurrentWeekStart] = useState(() => {
    const today = new Date();
    const day = today.getDay();
    const diff = today.getDate() - day + (day === 0 ? -6 : 1);
    return new Date(today.setDate(diff));
  });

  const dayNames = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const monthNames = [
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  ];

  const getWeekDays = () => {
    const days: Date[] = [];
    const start = new Date(currentWeekStart);
    for (let i = 0; i < 7; i++) {
      const day = new Date(start);
      day.setDate(start.getDate() + i);
      days.push(day);
    }
    return days;
  };

  const isDateDisabled = (date: Date): boolean => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const checkDate = new Date(date);
    checkDate.setHours(0, 0, 0, 0);
    return checkDate < today;
  };

  const isDateSelected = (date: Date): boolean => {
    if (!selectedDate) return false;
    return (
      date.getDate() === selectedDate.getDate() &&
      date.getMonth() === selectedDate.getMonth() &&
      date.getFullYear() === selectedDate.getFullYear()
    );
  };

  const getSlotsForDate = (date: Date): AvailableTimeSlot[] => {
    if (selectedDate && isDateSelected(date)) {
      return availableSlots;
    }
    return [];
  };

  const formatDayLabel = (date: Date): string => {
    const today = new Date();
    const isToday = date.toDateString() === today.toDateString();
    const dayNum = date.getDate();
    const monthName = date.toLocaleDateString("en-US", { month: "short" });
    return isToday ? "TODAY" : `${dayNames[date.getDay() === 0 ? 6 : date.getDay() - 1]} ${dayNum}`;
  };

  const goToPreviousWeek = () => {
    const newDate = new Date(currentWeekStart);
    newDate.setDate(newDate.getDate() - 7);
    setCurrentWeekStart(newDate);
  };

  const goToNextWeek = () => {
    const newDate = new Date(currentWeekStart);
    newDate.setDate(newDate.getDate() + 7);
    setCurrentWeekStart(newDate);
  };

  const goToToday = () => {
    const today = new Date();
    const day = today.getDay();
    const diff = today.getDate() - day + (day === 0 ? -6 : 1);
    const monday = new Date(today.setDate(diff));
    setCurrentWeekStart(monday);
  };

  const handleMonthChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const selectedMonth = parseInt(e.target.value);
    const newDate = new Date(currentWeekStart);
    newDate.setMonth(selectedMonth);
    const day = newDate.getDay();
    const diff = newDate.getDate() - day + (day === 0 ? -6 : 1);
    const monday = new Date(newDate.setDate(diff));
    setCurrentWeekStart(monday);
  };

  const weekDays = getWeekDays();
  const currentMonthIndex = currentWeekStart.getMonth();
  const currentYear = currentWeekStart.getFullYear();

  const months = monthNames.map((name, index) => ({
    value: index,
    label: name
  }));

  return (
    <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-2">
          <button
            type="button"
            onClick={goToPreviousWeek}
            className="p-2 hover:bg-gray-700 rounded-lg transition-colors"
          >
            <HiChevronLeft className="w-5 h-5 text-gray-400" />
          </button>
          <select
            value={currentMonthIndex}
            onChange={handleMonthChange}
            className="bg-gray-700 text-white text-base font-semibold px-4 py-2 rounded-lg border border-gray-600 hover:bg-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500 cursor-pointer"
          >
            {months.map((month) => (
              <option key={month.value} value={month.value}>
                {month.label}
              </option>
            ))}
          </select>
          <button
            type="button"
            onClick={goToNextWeek}
            className="p-2 hover:bg-gray-700 rounded-lg transition-colors"
          >
            <HiChevronRight className="w-5 h-5 text-gray-400" />
          </button>
        </div>
        <button
          type="button"
          onClick={goToToday}
          className="text-sm text-blue-400 hover:text-blue-300 px-3 py-2 rounded hover:bg-gray-700 transition-colors"
        >
          Back to Today
        </button>
      </div>

      <div className="grid grid-cols-7 gap-5 mb-8">
        {weekDays.map((day, dayIndex) => {
          const disabled = isDateDisabled(day);
          const selected = isDateSelected(day);
          const isToday = day.toDateString() === new Date().toDateString();
          const isDifferentMonth = day.getMonth() !== currentWeekStart.getMonth();

          return (
            <button
              key={day.toISOString()}
              type="button"
              onClick={() => !disabled && onDateSelect(day)}
              disabled={disabled}
              className={`
                px-4 py-3 rounded-lg text-base font-semibold transition-all
                ${disabled
                  ? "text-gray-600 cursor-not-allowed bg-gray-700/30"
                  : selected
                  ? "bg-blue-600 text-white shadow-md"
                  : isToday
                  ? "bg-gray-700 text-white border-2 border-blue-500"
                  : "bg-gray-700/50 text-gray-300 hover:bg-gray-700 hover:text-white"
                }
              `}
            >
              <div className="text-center">
                <div className="font-semibold">{formatDayLabel(day)}</div>
                {isDifferentMonth && (
                  <div className="text-xs opacity-75 mt-1">
                    {day.toLocaleDateString("en-US", { month: "short" })}
                  </div>
                )}
              </div>
            </button>
          );
        })}
      </div>

      {selectedDate ? (
        <div className="mt-6">
          {loading ? (
            <div className="text-base text-gray-500 text-center py-10">Loading time slots...</div>
          ) : availableSlots.length > 0 ? (
            <div className="grid grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-3">
              {availableSlots
                .filter((slot) => slot.available)
                .map((slot) => (
                  <button
                    key={slot.time}
                    type="button"
                    onClick={() => onTimeSelect(slot.time)}
                    className={`
                      px-5 py-4 rounded-lg text-base font-medium transition-all text-center
                      ${selectedTime === slot.time
                        ? "bg-blue-600 text-white shadow-lg border-2 border-orange-400"
                        : "bg-gray-700/60 text-gray-300 hover:bg-gray-600 hover:text-white border border-gray-600 hover:border-gray-500"
                      }
                    `}
                  >
                    {slot.time}
                  </button>
                ))}
            </div>
          ) : (
            <div className="text-base text-gray-500 text-center py-10">No slots available for this date</div>
          )}
        </div>
      ) : (
        <div className="text-base text-gray-600 text-center py-10">Select a date to see available time slots</div>
      )}
    </div>
  );
}

