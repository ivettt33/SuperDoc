import React from "react";
import { HiClock } from "react-icons/hi";

interface TimeSlot {
  time: string;
  available: boolean;
}

interface TimeSlotPickerProps {
  selectedTime: string | null;
  onTimeSelect: (time: string) => void;
  availableSlots: TimeSlot[];
  loading?: boolean;
}

const generateDefaultSlots = (): TimeSlot[] => {
  const slots: TimeSlot[] = [];
  for (let hour = 9; hour < 18; hour++) {
    for (let minute = 0; minute < 60; minute += 30) {
      const timeString = `${hour.toString().padStart(2, "0")}:${minute.toString().padStart(2, "0")}`;
      slots.push({ time: timeString, available: true });
    }
  }
  return slots;
};

export default function TimeSlotPicker({
  selectedTime,
  onTimeSelect,
  availableSlots = generateDefaultSlots(),
  loading = false,
}: TimeSlotPickerProps) {
  if (loading) {
    return (
      <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
        <div className="flex items-center justify-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-blue-500"></div>
        </div>
      </div>
    );
  }

  return (
    <div className="bg-gray-800 rounded-xl p-6 border border-gray-700">
      <div className="flex items-center space-x-2 mb-4">
        <HiClock className="w-5 h-5 text-gray-400" />
        <h3 className="text-lg font-semibold text-white">Available Time Slots</h3>
      </div>

      {availableSlots.length === 0 ? (
        <div className="text-center py-8">
          <p className="text-gray-400">No available time slots for this date</p>
        </div>
      ) : (
        <div className="grid grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">
          {availableSlots.map((slot) => (
            <button
              key={slot.time}
              type="button"
              onClick={() => slot.available && onTimeSelect(slot.time)}
              disabled={!slot.available}
              className={`
                px-4 py-3 rounded-lg font-medium transition-all duration-200
                ${!slot.available
                  ? "bg-gray-700/30 text-gray-600 cursor-not-allowed border border-gray-700"
                  : selectedTime === slot.time
                  ? "bg-blue-600 text-white shadow-lg shadow-blue-500/30 scale-105"
                  : "bg-gray-700 text-gray-300 hover:bg-gray-600 hover:text-white border border-gray-600"
                }
              `}
            >
              {slot.time}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
