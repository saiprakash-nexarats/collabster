import test from 'node:test';
import assert from 'node:assert/strict';
import { genderOptions, countryCityMap } from '../src/geo.mjs';

test('genderOptions contains inclusive genders and includes "Prefer not to say"', () => {
  assert.ok(Array.isArray(genderOptions), 'genderOptions should be an array');
  assert.ok(genderOptions.includes('Prefer not to say'), 'Must include "Prefer not to say"');
  assert.ok(genderOptions.includes('Female'), 'Must include "Female"');
  assert.ok(genderOptions.includes('Male'), 'Must include "Male"');
  assert.ok(genderOptions.includes('Non-binary'), 'Must include "Non-binary"');
  assert.ok(genderOptions.length >= 7, 'Should have multiple inclusive gender options');
});

test('countryCityMap contains comprehensive global countries with extracted city recommendations', () => {
  const countries = Object.keys(countryCityMap);
  assert.ok(countries.length >= 20, 'Should include at least 20 countries worldwide');
  
  const expectedCountries = [
    'India',
    'United States',
    'United Kingdom',
    'Canada',
    'Australia',
    'Germany',
    'France',
    'Japan',
    'Singapore',
    'United Arab Emirates',
  ];

  for (const country of expectedCountries) {
    assert.ok(countries.includes(country), `Should contain ${country}`);
    const cities = countryCityMap[country];
    assert.ok(Array.isArray(cities) && cities.length > 0, `${country} should have city recommendations`);
  }

  // Check specific city recommendations
  assert.ok(countryCityMap['India'].includes('Bengaluru'));
  assert.ok(countryCityMap['India'].includes('Mumbai'));
  assert.ok(countryCityMap['United States'].includes('San Francisco'));
  assert.ok(countryCityMap['United States'].includes('New York'));
  assert.ok(countryCityMap['United Kingdom'].includes('London'));
});
